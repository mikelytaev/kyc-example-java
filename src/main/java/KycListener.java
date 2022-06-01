import java.util.*;
import java.util.concurrent.ExecutionException;

import com.sirius.sdk.agent.aries_rfc.feature_0036_issue_credential.messages.ProposedAttrib;
import com.sirius.sdk.agent.aries_rfc.feature_0036_issue_credential.state_machines.Issuer;
import com.sirius.sdk.agent.aries_rfc.feature_0095_basic_message.Message;
import com.sirius.sdk.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest;
import com.sirius.sdk.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation;
import com.sirius.sdk.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter;
import com.sirius.sdk.agent.connections.Endpoint;
import com.sirius.sdk.agent.ledger.CredentialDefinition;
import com.sirius.sdk.agent.ledger.Schema;
import com.sirius.sdk.agent.listener.Event;
import com.sirius.sdk.agent.listener.Listener;
import com.sirius.sdk.agent.pairwise.Pairwise;
import com.sirius.sdk.hub.CloudContext;
import com.sirius.sdk.hub.Context;
import com.sirius.sdk.utils.Pair;
import org.json.JSONObject;

import javax.print.Doc;

public class KycListener {

    static Map<String/*connection key*/, Document> documents = new HashMap<>();

    public static void listener() {
        try (Context context = CloudContext.builder().
                setServerUri(Consts.serverUri).
                setCredentials(Consts.credentials).
                setP2p(Consts.p2pConnection).build()) {
            Pair<CredentialDefinition, Schema> credInfo = Document.regCreds(context, Consts.publicDid, Consts.DKMS_NAME);

            Pair<String, String> didVerkey = context.getDid().createAndStoreMyDid();
            String myDid = didVerkey.first;
            String myVerkey = didVerkey.second;
            Endpoint myEndpoint = context.getEndpointWithEmptyRoutingKeys();

            Listener listener = context.subscribe();
            while (true) {
                Event event = listener.getOne().get();
                System.out.println("received: " + event.message().getMessageObj().toString());
                
                if (documents.containsKey(event.getRecipientVerkey()) &&
                        event.message() instanceof ConnRequest) {
                    ConnRequest request = (ConnRequest) event.message();
                    String connectionKey = event.getRecipientVerkey();
                    Inviter sm = new Inviter(context, new Pairwise.Me(myDid, myVerkey), connectionKey, myEndpoint);
                    Pairwise p2p = sm.createConnection(request);
                    if (p2p != null) {
                        Message hello = Message.builder().
                                setContent("Welcome to the KYC provider!").
                                setLocale("en").
                                build();
                        context.sendTo(hello, p2p);

                        Issuer issuerMachine = new Issuer(context, p2p, 600);
                        String credId = "cred-id-" + UUID.randomUUID();

                        Document doc = documents.get(connectionKey);

                        List<ProposedAttrib> preview = new ArrayList<>();
                        JSONObject docValues = doc.toIndyValues();
                        for (String key : docValues.keySet()) {
                            preview.add(new ProposedAttrib(key, docValues.get(key).toString()));
                        }

                        boolean ok = issuerMachine.issue(new Issuer.IssueParams().
                                setValues(docValues).
                                setSchema(credInfo.second).
                                setCredDef(credInfo.first).
                                setComment("Here is your digital document").
                                setPreview(preview).
                                setCredId(credId));
                        if (ok) {
                            System.out.println("Document was successfully issued");
                            documents.remove(connectionKey);
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadDoc(Document doc) {
        try (Context context = CloudContext.builder().
                setServerUri(Consts.serverUri).
                setCredentials(Consts.credentials).
                setP2p(Consts.p2pConnection).build()) {
            String connectionKey = context.getCrypto().createKey();
            Endpoint myEndpoint = context.getEndpointWithEmptyRoutingKeys();
            Invitation invitation = Invitation.builder().
                    setLabel("KYC provider").
                    setRecipientKeys(Collections.singletonList(connectionKey)).
                    setEndpoint(myEndpoint.getAddress()).
                    build();
            String qrContent = context.generateQrCode(invitation.invitationUrl());
            documents.put(connectionKey, doc);
            return qrContent;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(()->listener()).start();
        Thread.sleep(5000);

        // Получаем от фронтенда информацию о документе, который нужно выдать
        Document doc = new Document();
        doc.firstName = "Mike";
        doc.lastName = "L.";
        doc.number = "123ABC";
        String qr = loadDoc(doc);
        System.out.println("Scan this QR by Sirius App " + qr);
    }
}

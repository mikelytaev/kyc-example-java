import com.sirius.sdk.agent.ledger.CredentialDefinition;
import com.sirius.sdk.agent.ledger.Ledger;
import com.sirius.sdk.agent.ledger.Schema;
import com.sirius.sdk.agent.wallet.abstract_wallet.model.AnonCredSchema;
import com.sirius.sdk.hub.Context;
import com.sirius.sdk.utils.Pair;
import org.json.JSONObject;

public class Document {

    public String firstName;
    public String lastName;
    public String number;

    public JSONObject toIndyValues() {
        return new JSONObject().
                put("firstName", firstName).
                put("lastName", lastName).
                put("number", number);
    }

    public static Pair<CredentialDefinition, Schema> regCreds(Context issuer, String did, String dkmsName) {
        String schemaName = "document";
        Pair<String, AnonCredSchema> schemaPair = issuer.getAnonCreds().issuerCreateSchema(did, schemaName, "0.1",
                "firstName", "lastName", "number");
        AnonCredSchema anoncredSchema = schemaPair.second;
        Ledger ledger = issuer.getLedgers().get(dkmsName);

        Schema schema = ledger.ensureSchemaExists(anoncredSchema, did);

        if (schema == null) {
            Pair<Boolean, Schema> okSchema = ledger.registerSchema(anoncredSchema, did);
            if (okSchema.first) {
                System.out.println("Schema was registered successfully");
                schema = okSchema.second;
            } else {
                System.out.println("Schema was not registered");
                return null;
            }
        } else {
            System.out.println("Schema is already exists in the ledger");
        }

        Pair<Boolean, CredentialDefinition> okCredDef = ledger.registerCredDef(new CredentialDefinition("TAG", schema), did);
        CredentialDefinition credDef = okCredDef.second;

        return new Pair<>(credDef, schema);
    }

}

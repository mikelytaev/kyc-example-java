import com.sirius.sdk.encryption.P2PConnection;

import java.nio.charset.StandardCharsets;

public class Consts {
    static String serverUri = "https://demo.socialsirius.com";
    static byte[] credentials = "ez8ucxfrTiV1hPX99MHt/C/MUJCo8OmN4AMVmddE/sew8gBzsOg040FWBSXzHd9hDoj5B5KN4aaLiyzTqkrbD3uaeSwmvxVsqkC0xl5dtIc=".getBytes(StandardCharsets.UTF_8);
    static P2PConnection p2pConnection = new P2PConnection("6QvQ3Y5pPMGNgzvs86N3AQo98pF5WrzM1h6WkKH3dL7f",
            "28Au6YoU7oPt6YLpbWkzFryhaQbfAcca9KxZEmz22jJaZoKqABc4UJ9vDjNTtmKSn2Axfu8sT52f5Stmt7JD4zzh",
            "6oczQNLU7bSBzVojkGsfAv3CbXagx7QLUL7Yj1Nba9iw");
    static String publicDid = "Th7MpTaRZVRYnPiabds81Y";

    static final String DKMS_NAME = "test_network";
}

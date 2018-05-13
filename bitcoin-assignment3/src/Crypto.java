import java.security.*;

public class Crypto {

    /**
     * @return true is {@code signature} is a valid digital signature of {@code message} under the
     *         key {@code pubKey}. Internally, this uses RSA signature, but the student does not
     *         have to deal with any of the implementation details of the specific signature
     *         algorithm
     */
    public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature) {

        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sig.initVerify(pubKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            sig.update(message);
            boolean result = sig.verify(signature);

            //System.out.println("pubkey: " + pubKey.toString());
            //System.out.println("message: " + Utils.bytesToHex(message));
            //System.out.println("signature: " + Utils.bytesToHex(signature));
            //System.out.println("result: " + result);

            return result;
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        //System.out.println("Error?");
        return false;

    }

    public static byte[] generateSignature(PrivateKey privKey, byte[] message) {
        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sig.initSign(privKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            sig.update(message);
            return sig.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return null;
    }
}

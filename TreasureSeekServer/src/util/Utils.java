package util;

public class Utils {

	public static class Pair<K,V> {
		
		public K key;
		public V value;
		
		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
	}
	
	 public static void setSecurityProperties() {
		 String password = "123456";
		 System.setProperty("javax.net.ssl.keyStore", "../security/keys/keystore");
		 System.setProperty("javax.net.ssl.keyStorePassword", password);
		 
		 System.setProperty("javax.net.ssl.trustStore", "../security/certificates/truststore");
		 System.setProperty("javax.net.ssl.trustStorePassword", password);
		
	}
	
}

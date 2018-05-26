package util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;


public class Utils {

	public static class Pair<K, V> implements Serializable {

		private static final long serialVersionUID = -4589969236014340084L;
		public K key;
		public V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.value);
		}

		@Override
		public boolean equals(Object obj) {
			@SuppressWarnings("unchecked")
			Pair<K, V> pair2 = (Pair<K, V>) obj;

			return this.key.equals(pair2.key) && this.value.equals(pair2.value);
		}

	}

	public static void setSecurityProperties(boolean debugEclipse) {
		
		String keyStorePath = debugEclipse ? "security/keys/keystore" : "../security/keys/keystore";
		String trustStorePath = debugEclipse ? "security/certificates/truststore" : "../security/certificates/truststore";
		
		String password = "123456";
		System.setProperty("javax.net.ssl.keyStore", keyStorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", password);

		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", password);

	}
	
	public static String bindParamenter(String[] args, String prefix, String alternative, String usage) {

		String result = alternative;
		int index = Arrays.asList(args).indexOf(prefix);
		if(index != -1) {
			try {
				result = args[index + 1];				
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(usage);
				System.exit(1);
			}
		}
		
		return result;
	}

}

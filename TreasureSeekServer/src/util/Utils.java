package util;

import java.io.Serializable;
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

	public static void setSecurityProperties() {
		String password = "123456";
		System.setProperty("javax.net.ssl.keyStore", "../security/keys/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", password);

		System.setProperty("javax.net.ssl.trustStore", "../security/certificates/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", password);

	}

}

package util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {

	public static class Pair<K, V> {

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

	public static String getRealHost() throws SocketException {
		Enumeration en = NetworkInterface.getNetworkInterfaces();
		while (en.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) en.nextElement();
			Enumeration ee = ni.getInetAddresses();

			while (ee.hasMoreElements()) {
				InetAddress ia = (InetAddress) ee.nextElement();
				String iaAddress = ia.getHostAddress();
				if (validIP(iaAddress))
					return iaAddress;
			}
		}

		return "No valid IP address";
	}

	public static boolean validIP(String ip) {
		if (ip == null || ip.isEmpty())
			return false;
		ip = ip.trim();
		if ((ip.length() < 6) & (ip.length() > 15))
			return false;

		try {
			Pattern pattern = Pattern.compile(
					"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
			Matcher matcher = pattern.matcher(ip);
			return matcher.matches();
		} catch (PatternSyntaxException ex) {
			return false;
		}
	}

}

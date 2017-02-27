import java.util.*;
import java.io.*;
import javax.net.ssl.SSLContext;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.*;
import org.json.*;

public class Databox {

	private String arbiterURL;
	private String arbiterToken;

	private CloseableHttpClient httpClient;

	HashMap<String, String> tokenCache = new HashMap<String, String>();

	public Databox() {
		// TODO: Check env vars there
		Map<String, String> env = System.getenv();

		arbiterURL   = env.get("DATABOX_ARBITER_ENDPOINT");
		arbiterToken = env.get("ARBITER_TOKEN");

		httpClient = createHTTPSClient(env.get("CM_HTTPS_CA_ROOT_CERT"));
	}

	private CloseableHttpClient createHTTPSClient(String caRootCert) {
		File temp = null;
		try {
			temp = File.createTempFile("root-ca", ".cert");
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			bw.write(caRootCert);
			bw.close();
		} catch (IOException e) {
			System.err.println("Error while writing Databox root CA cert to file");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			sun.security.tools.keytool.Main.main(new String[]{
				"-import",
				"-noprompt",
				"-file", temp.getAbsolutePath(),
				"-alias", "ca",
				"-storepass", "nopassword",
				"-keystore", "truststore.keystore"
			});
		} catch (Exception e) {
			System.err.println("Error while generating truststore from Databox root CA cert file");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(new File("truststore.keystore"), "nopassword".toCharArray(),
						new TrustSelfSignedStrategy())
				.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslcontext,
					new String[] { "TLSv1" },
					null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			return HttpClients.custom()
				.setSSLSocketFactory(sslsf)
				.build();
		} catch (Exception e) {
			System.err.println("Unble to build HTTPClient with custom SSL root CA cert");
			e.printStackTrace();
			System.exit(1);
		}

		return HttpClients.createDefault();
	}

	private String requestToken(String hostname, String endpoint, String method) {
		HttpPost request = new HttpPost(arbiterURL + "/token");
		JSONObject json = new JSONObject();
		json.put("target", hostname);
		json.put("path",   endpoint);
		json.put("method", method);

		CloseableHttpResponse response = null;

		try {
			request.addHeader("Content-Type", "application/json");
			request.addHeader("X-Api-Key", arbiterToken);
			request.setEntity(new StringEntity(json.toString()));
			response = httpClient.execute(request);
		} catch (Exception e) {
			System.err.println("Error while executing arbiter token request");
			e.printStackTrace();
			System.exit(1);
		}

		String token = null;

		try {
			//System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();

			String body = EntityUtils.toString(entity);
			//System.out.println(body);

			// TODO: Error handling
			token = body;

			EntityUtils.consume(entity);
		} catch (Exception e) {
			System.err.println("Error parsing arbiter token request response");
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				response.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return token;
	}

	public JSONObject getRootCatalog() {
		HttpGet request = new HttpGet(arbiterURL + "/cat");
		CloseableHttpResponse response = null;

		try {
			request.addHeader("X-Api-Key", arbiterToken);
			response = httpClient.execute(request);
		} catch (Exception e) {
			System.err.println("Error while requesting root catalog");
			e.printStackTrace();
			System.exit(1);
		}

		JSONObject cat = null;

		try {
			//System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();

			String body = EntityUtils.toString(entity);
			//System.out.println(body);

			// TODO: Error handling
			cat = new JSONObject(body);

			EntityUtils.consume(entity);
		} catch (Exception e) {
			System.err.println("Error parsing arbiter root Hypercat catalog");
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				response.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return cat;
	}
}

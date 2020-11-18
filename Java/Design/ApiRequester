import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.codec.binary.Base64;
import com.google.common.base.Joiner;



/**
 * 接口工具类
 * @author hoojo
 * @createDate 2019年9月25日 下午4:24:13
 * @blog http://hoojo.cnblogs.com
 * @email hoojo_@126.com
 * @version 1.0
 */
public class ApiRequester {

	private static final String BMD_COP_SIGNVERSION = "BMD-COP-SIGNVERSION";
	private static final String BMD_COP_PRODUCTKEY = "BMD-COP-PRODUCTKEY";
	private static final String BMD_COP_APPKEY = "BMD-COP-APPKEY";
	private static final String BMD_COP_TIMESTAMP = "BMD-COP-TIMESTAMP";
	private static final String BMD_COP_NONCE = "BMD-COP-NONCE";
	private static final String BMD_RSP_SIGNATURE = "BMD-RSP-SIGNATURE";
	public static final String BMD_COP_SIGNATURE = "BMD-COP-SIGNATURE";
	
	private static final String BASE_URL = "http://xxx.api.com/";
	private static final String PRODUCT_KEY = "p3";
	private static final String APP_KEY = "xxx";
	private static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCNnpr/265/A=";
	private static final String BACK_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgP0YU0D9kbq+";
	public static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjZ6a/";
	
    private String getSignParams(String nonce, long timestamp, SortedMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("product_key=").append(PRODUCT_KEY)
	        .append("&app_key=").append(APP_KEY)
	        .append("&nonce=").append(nonce)
	        .append("&timestamp=").append(timestamp)
	        .append("&").append(Joiner.on('&').withKeyValueSeparator("=").join(params));
        
        return sb.toString();
    }

    public ApiRequestBuilder build(String api) {
        return new ApiRequestBuilder(api);
    }
    
    public static boolean verify(PublicKey publicKey, String data, String signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        
        return sig.verify(Base64.decodeBase64(signature));
    }

    private String sign(PrivateKey privateKey, String data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(data.getBytes(Charset.forName("UTF-8")));
        
        return Base64.encodeBase64String(sig.sign());
    }
    
    private PrivateKey readPrivateKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decodeBase64(content));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        
        return kf.generatePrivate(spec);
    }

	public static PublicKey readPublicKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(content));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        
        return kf.generatePublic(spec);
    }
    
    public class ApiRequestBuilder {
        
        private String api;
        private SortedMap<String, String> textParams;
        private MultipartEntityBuilder entityBuilder;

        private ApiRequestBuilder(String api) {
            this.api = api;
            this.textParams = new TreeMap<>();
            this.entityBuilder = MultipartEntityBuilder.create();
        }

        public ApiRequestBuilder addFile(String name, File file, ContentType contentType) {
            entityBuilder.addBinaryBody(name, file, contentType, file.getName());
            return this;
        }
        
        public ApiRequestBuilder addText(String name, String value) {
            if (!textParams.containsKey(name)) {
                textParams.put(name, value);
                entityBuilder.addTextBody(name, value, ContentType.create("text/plain", Charset.forName("UTF-8")));
            }

            return this;
        }
        
        public ApiRequestBuilder addParams(Map<String, String> params, String... ignores) {
        	Set<String> keys = params.keySet();
    		
        	Iterator<String> iter = keys.iterator();
    		while (iter.hasNext()) {
    			String key = iter.next();
    			if (ArrayUtils.contains(ignores, key)) {
    				continue;
    			}
    			
    			String val = params.get(key);
    			if (StringUtils.isNotBlank(val)) {
    				addText(key, val);
    			}
    		}
        	
            return this;
        }
        
        public ApiRequestBuilder addEntity(Object entity, String... ignores) {
        	try {
				addParams(BeanUtils.describe(entity), ignores);
			} catch (Exception e) {
				ILogUtil.error("接口请求异常", e);
    			throw new Exception(SystemStateCode.Common.SYSTEM_ERROR);
			} 
        	
            return this;
        }
        
        public String post() throws Exception {
        	long timestamp = System.currentTimeMillis();
            String nonce = RandomStringUtils.randomAlphanumeric(16);
            
            String signData = getSignParams(nonce, timestamp, textParams);
            String signature = sign(readPrivateKey(PRIVATE_KEY), signData);

            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost(BASE_URL + api);
            httpPost.addHeader(BMD_COP_SIGNVERSION, "1");
            httpPost.addHeader(BMD_COP_PRODUCTKEY, PRODUCT_KEY);
            httpPost.addHeader(BMD_COP_APPKEY, APP_KEY);
            httpPost.addHeader(BMD_COP_NONCE, nonce);
            httpPost.addHeader(BMD_COP_TIMESTAMP, String.valueOf(timestamp));
            httpPost.addHeader(BMD_COP_SIGNATURE, signature);
            httpPost.setEntity(entityBuilder.build());

            CloseableHttpResponse response = null;
            String bmdRspContent = null;
            
            try {
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        			throw new MgException(SystemStateCode.Common.SYSTEM_ERROR, "调用接口异常，请求状态码非正常状态");
                }
                
                if (null != response.getEntity()) {
                    Charset charset = getCharset(response.getEntity());
                    InputStream is = response.getEntity().getContent();
                    Header[] contentEncoding = response.getHeaders("content-encoding");
                    for (Header header : contentEncoding) {
                        if (header.getValue().contains("gzip")) {
                            is = new GZIPInputStream(is);
                            break;
                        }
                    }
                    
                    bmdRspContent = IOUtils.toString(is, charset);
                }
            } catch (Exception e) {
            	ILogUtil.error("接口请求异常", e);
    			throw new Exception(SystemStateCode.Common.SYSTEM_ERROR);
            }
            
            String bmdRspSignature = response.getHeaders(BMD_RSP_SIGNATURE)[0].getValue();
            if (verify(readPublicKey(BACK_PUBLIC_KEY), bmdRspContent, bmdRspSignature)) {
                return bmdRspContent;
            }
            
            throw new Exception("签名验证错误");
        }

        private Charset getCharset(HttpEntity entity) {
            Charset charset = StandardCharsets.UTF_8;
            ContentType contentType = ContentType.get(entity);
            if (null != contentType) {
                charset = contentType.getCharset();
            }
            
            return charset;
        }
    }
}

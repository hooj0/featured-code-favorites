public class HttpRequester {

	private static final String BASE_URL = "http://www.api.com/";
	private static final String PRODUCT_KEY = "v1";
	private static final String APP_KEY = "abcd";
	private static final String PRIVATE_KEY = "AjsWDb3A=";
	private static final String BACK_PUBLIC_KEY = "QIDAQAB";
	@SuppressWarnings("unused")
	private static final String PUBLIC_KEY = "vwIDAQAB";
	
    private String getSignParams(String nonce, long timestamp, SortedMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("product_key=").append(PRODUCT_KEY)
	        .append("&app_key=").append(APP_KEY)
	        .append("&nonce=").append(nonce)
	        .append("&timestamp=").append(timestamp)
	        .append("&").append(Joiner.on('&').withKeyValueSeparator("=").join(params));
        
        return sb.toString();
    }

    public HttpRequestBuilder build(String api) {
	    
        return new HttpRequestBuilder(api);
    }

    public class HttpRequestBuilder {
        
    	private static final String BMD_COP_SIGNVERSION = "BMD-COP-SIGNVERSION";
    	private static final String BMD_COP_PRODUCTKEY = "BMD-COP-PRODUCTKEY";
    	private static final String BMD_COP_APPKEY = "BMD-COP-APPKEY";
    	private static final String BMD_COP_TIMESTAMP = "BMD-COP-TIMESTAMP";
    	private static final String BMD_COP_NONCE = "BMD-COP-NONCE";
    	private static final String BMD_COP_SIGNATURE = "BMD-COP-SIGNATURE";
    	private static final String BMD_RSP_SIGNATURE = "BMD-RSP-SIGNATURE";
        
        private String api;
        private SortedMap<String, String> textParams;
        private MultipartEntityBuilder entityBuilder;

        private HttpRequestBuilder(String api) {
            this.api = api;
            this.textParams = new TreeMap<>();
            this.entityBuilder = MultipartEntityBuilder.create();
        }

        public HttpRequestBuilder addText(String name, String value) {
            if (!textParams.containsKey(name)) {
                textParams.put(name, value);
                entityBuilder.addTextBody(name, value, ContentType.create("text/plain", Charset.forName("UTF-8")));
            }

            return this;
        }
        
        public HttpRequestBuilder addParams(Map<String, String> params) {
        	Set<String> keys = params.keySet();
    		
        	Iterator<String> iter = keys.iterator();
    		while (iter.hasNext()) {
    			String key = iter.next();
    			String val = params.get(key);
    			if (StringUtils.isNotBlank(val)) {
    				addText(key, val);
    			}
    		}
        	
            return this;
        }
        
        public HttpRequestBuilder addEntity(Object entity) {
        	try {
				addParams(BeanUtils.describe(entity));
			} catch (Exception e) {
				ILogUtil.error("白猫贷接口请求异常", e);
    			throw new MgException(EmSystemStateCode.Common.SYSTEM_ERROR);
			} 
        	
            return this;
        }

        public HttpRequestBuilder addFile(String name, File file, ContentType contentType) {
            entityBuilder.addBinaryBody(name, file, contentType, file.getName());
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
        			throw new MgException(EmSystemStateCode.Common.SYSTEM_ERROR, "调用接口异常，请求状态码非正常状态");
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
            	ILogUtil.error("白猫贷接口请求异常", e);
    			throw new MgException(EmSystemStateCode.Common.SYSTEM_ERROR);
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
        
        private boolean verify(PublicKey publicKey, String data, String signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
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

    	private PublicKey readPublicKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(content));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            
            return kf.generatePublic(spec);
        }
    }
}

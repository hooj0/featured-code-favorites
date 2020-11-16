import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * HTTP请求转发代理
 * @author hoojo
 * @createDate 2020年11月16日 上午11:18:17
 * @version 1.0
 */
public class ForwardProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_TARGET_URL = "targetURL";
	private static final String PARAM_READ_TIMEOUT = "readTimeout";
	private static final String PARAM_CONNECT_TIMEOUT = "connectTimeout";
	private static final String PARAM_COPY_REQUEST_HEADER = "copyRequestHeader";
	
	private String url;
	private int readTimeout = 0;
	private int connectTimeout = 0;
	private boolean copyRequestHeader = false;
	
	private void process(HttpServletRequest request, HttpServletResponse response, String[] target) throws MalformedURLException, IOException {
		//System.out.println("proxy " + request.getMethod() + " uri: " + request.getRequestURI() + " --> " + url);
		
		// 取得连接
		HttpURLConnection connect = (HttpURLConnection) new URL(url + target[0]).openConnection();
		
		// 设置连接属性
		connect.setDoOutput(true);
		connect.setRequestMethod("POST");
		connect.setUseCaches(false);
		connect.setInstanceFollowRedirects(true);
		connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		if (connectTimeout > 0) {
			connect.setConnectTimeout(connectTimeout * 1000);
		}
		if (readTimeout > 0) {
			connect.setReadTimeout(readTimeout * 1000);
		}
		if (copyRequestHeader) {
			addConnectionHeader(request, connect);
		}
		connect.connect();
		
		// 往目标 url 中提供参数
		OutputStream output = connect.getOutputStream();
		output.write(target[1].getBytes());
		output.flush();
		output.close();
		
		// 取得页面输出，并设置页面编码及缓存设置
		response.setContentType(connect.getContentType());
		response.setHeader("Cache-Control", connect.getHeaderField("Cache-Control"));
		response.setHeader("Pragma", connect.getHeaderField("Pragma"));
		response.setHeader("Expires", connect.getHeaderField("Expires"));
		
		OutputStream os = response.getOutputStream();
		
		// 将目标  url 的输入流回写返回数据
		InputStream is = connect.getInputStream();
		int read;
		while ((read = is.read()) != -1) {
			os.write(read);
		}
		
		is.close();
		os.flush();
		os.close();
		connect.disconnect();
	}
	
	private void addConnectionHeader(HttpServletRequest request, HttpURLConnection connect) {
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String header = headerNames.nextElement();
			if ("Content-Type".equals(header)) {
				continue;
			}
			
			connect.addRequestProperty(header, request.getHeader(header));
		}
	}

	/**
	 * 将参数中的目标分离成由目标  名称和参数组成的数组
	 */
	private String[] parse(Map<String, String[]> map) throws UnsupportedEncodingException {
		String[] params = { "", "" };
		
		Iterator<Entry<String, String[]>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String[]> item = iter.next();
			String[] values = item.getValue();
			if ("servletName".equals(item.getKey())) {
				// 取出servlet名称
				params[0] = values[0];
			} else {
				// 重新组装参数字符串
				for (int i = 0; i < values.length; i++) {
					// 参数需要进行转码，实现字符集的统一
					params[1] += "&" + item.getKey() + "=" + URLEncoder.encode(values[i], "utf-8");
				}
			}
		}
		params[1] = params[1].replaceAll("^&", "");
		return params;
	}

	@Override
	public void init() throws ServletException {
		url = this.getInitParameter(PARAM_TARGET_URL);
		copyRequestHeader = BooleanUtils.toBoolean(this.getInitParameter(PARAM_COPY_REQUEST_HEADER));
		readTimeout = Integer.parseInt(StringUtils.defaultIfBlank(this.getInitParameter(PARAM_READ_TIMEOUT), "0"));
		connectTimeout = Integer.parseInt(StringUtils.defaultIfBlank(this.getInitParameter(PARAM_CONNECT_TIMEOUT), "0"));
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] target = parse(request.getParameterMap());
		process(request, response, target);
	}
}

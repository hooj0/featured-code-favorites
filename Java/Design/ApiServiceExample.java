import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Service;

import com.alibaba.druid.support.json.JSONUtils;


/**
 * 接口对接服务
 * @author hoojo
 * @createDate 2019年9月25日 下午1:50:17
 * @blog http://hoojo.cnblogs.com
 * @email hoojo_@126.com
 * @version 1.0
 */
@Service
public class ApiServiceExample {

	private final static ContentType JPG = ContentType.parse("image/jpeg");
	private final static ContentType PDF = ContentType.parse("application/pdf");
	
	private final static String LOAN_CONFIG_NO = "943849E91C8B";
	private final static String PAY_MERCHANT_NO = "0FBF0BF4533A";
	
	
	
	/**
	 * 用户注册
	 * @author hoojo
	 * @createDate 2019年9月25日 下午5:36:28
	 */
	public ResponseVo<?> register(RequestVo requestVo, String sessionid) {
		try {
			String data = new ApiRequester().build("/api/v1/user_create")
					.addEntity(requestVo)
					.post();
			ILogUtil.info("创建个人账户返回信息："+ data);
			
			return new ResponseVo<>(JSONUtils.parse(data));
		} catch (Exception e) {
			ILogUtil.error(e.getMessage(), e);
			throw new Exception(SystemStateCode.Common.SYSTEM_ERROR, "创建个人账户异常");
		}
	}
	
	/**
	 * 用户身份认证
	 * @author hoojo
	 * @createDate 2019年9月25日 下午5:36:28
	 */
	public ResponseVo<?> identifyUser(IdentifyUserRequestVo requestVo, String sessionid) {
		
		try {
			String data = new ApiRequester().build("/api/v1/user_identify")
					.addEntity(requestVo)
					.addText("idcardFrontImgMd5", DigestUtils.md5Hex(new FileInputStream(requestVo.getIdcardFrontImgFile())).toUpperCase())
					.addFile("idcardFrontImgFile", new File(requestVo.getIdcardFrontImgFile()), JPG)
					.addText("idcardBackImgMd5", DigestUtils.md5Hex(new FileInputStream(requestVo.getIdcardBackImgFile())).toUpperCase())
					.addFile("idcardBackImgFile", new File(requestVo.getIdcardBackImgFile()), JPG)
					.post();
			
			return new ResponseVo<>(JSONUtils.parse(data));
		} catch (Exception e) {
			ILogUtil.error(e.getMessage(), e);
			throw new Exception(SystemStateCode.Common.SYSTEM_ERROR, "账户认证异常");
		}
	}

}

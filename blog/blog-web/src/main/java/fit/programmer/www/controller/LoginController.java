package fit.programmer.www.controller;

import com.qq.connect.QQConnectException;
import com.qq.connect.api.OpenID;
import com.qq.connect.api.qzone.UserInfo;
import com.qq.connect.javabeans.AccessToken;
import com.qq.connect.javabeans.qzone.UserInfoBean;
import com.qq.connect.oauth.Oauth;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import fit.programmer.www.common.Constants;
import fit.programmer.www.common.MD5Util;
import fit.programmer.www.common.RandStringUtils;
import fit.programmer.www.entity.OpenUser;
import fit.programmer.www.entity.User;
import fit.programmer.www.service.OpenUserService;
import fit.programmer.www.service.UserInfoService;
import fit.programmer.www.service.UserService;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by wly on 2018/4/21.
 */
@Controller
public class LoginController extends BaseController {
    private final static Logger log = Logger.getLogger( LoginController.class);

    @Autowired
    private UserService userService;
    @Autowired// redis数据库操作模板
    private RedisTemplate<String, String> redisTemplate;// jdbcTemplate HibernateTemplate

    @Autowired
    @Qualifier("jmsQueueTemplate")
    private JmsTemplate jmsTemplate;// mq消息模板.

    @Autowired
    private OpenUserService openUserService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 点击qq登陆
     * @param model
     * @return
     */
    @RequestMapping("/to_login")
    public String toLogin(Model model) {
        HttpServletRequest request = getRequest();
        String url = "";
        try {
            url = new Oauth().getAuthorizeURL(request);
        } catch (QQConnectException e) {
            e.printStackTrace();
        }
        return "redirect:"+url;
    }

    /**
     * qq授权登录
     * @param model
     * @return
     */
    @RequestMapping("/qq_login")
    public String qqLogin(Model model) {
        User user = getCurrentUser();
        boolean flag =false;
        try {
            AccessToken accessTokenObj = (new Oauth()).getAccessTokenByRequest(getRequest());
            String accessToken   = null, openID   = null;
            long tokenExpireIn = 0L;
            if (accessTokenObj.getAccessToken().equals("")) {
                System.out.print("没有获取到响应参数");
            } else {
                accessToken = accessTokenObj.getAccessToken();//授权令牌token
                tokenExpireIn = accessTokenObj.getExpireIn();//过期时间

                // 利用获取到的accessToken 去获取当前用的openid -------- start
                OpenID openIDObj =  new OpenID(accessToken);
                openID = openIDObj.getUserOpenID();//用户唯一标识
                // 利用获取到的accessToken 去获取当前用户的openid --------- end

                UserInfo qzoneUserInfo = new UserInfo(accessToken, openID);
                UserInfoBean userInfoBean = qzoneUserInfo.getUserInfo();
                if (userInfoBean.getRet() == 0) {
                    OpenUser openUser =  openUserService.findByOpenId( openID );
                    if(openUser == null){
                        redisTemplate.opsForValue().set(openID, accessToken, 90, TimeUnit.DAYS);// 有效期三个月
                        openUser = new OpenUser();
                        if(user==null){
                            flag = true;
                            user = new User();
                            user.setEmail( openID );
                            user.setPassword(MD5Util.encodeToHex(Constants.SALT+accessToken) );
                            user.setEnable( "1" );
                            user.setState("0");
                            user.setNickName( userInfoBean.getNickname() );//设置qq昵称
                            user.setImgUrl( userInfoBean.getAvatar().getAvatarURL50() );//设置qq头像
                            userService.regist( user );
                        }
                        openUser.setOpenId( openID );
                        openUser.setAccessToken( accessToken );
                        openUser.setAvatar( userInfoBean.getAvatar().getAvatarURL50() );
                        openUser.setExpiredTime( tokenExpireIn);
                        openUser.setNickName( userInfoBean.getNickname() );
                        openUser.setOpenType( Constants.OPEN_TYPE_QQ );
                        openUser.setuId( user.getId());
                        openUserService.add( openUser );
                    }else {
                        String token = redisTemplate.opsForValue().get( openID );//从redis获取accessToken
                        if(token==null){
                            //已过期
                            openUser.setAccessToken( accessToken );
                            openUser.setAvatar( userInfoBean.getAvatar().getAvatarURL50() );
                            openUser.setExpiredTime( tokenExpireIn);
                            openUser.setNickName( userInfoBean.getNickname() );
                            openUserService.update(openUser);
                        }
                        user = userService.findById( openUser.getuId() );
                    }

                } else {
                    log.info("很抱歉，我们没能正确获取到您的信息，原因是： " + userInfoBean.getMsg());
                }

            }
        } catch (QQConnectException e) {
            e.printStackTrace();
        }
        getSession().setAttribute("user",user);
        if(flag){
            return "redirect:/list";
        }else {
            model.addAttribute("qq",Constants.OPEN_TYPE_QQ );
            fit.programmer.www.entity.UserInfo userInfo =   userInfoService.findByUid(user.getId());
            model.addAttribute("user",user);
            model.addAttribute("userInfo",userInfo);
            return "personal/profile";
        }

    }

    @RequestMapping("/login")
    public String login(Model model) {
        User user = getCurrentUser();
        if(user!=null){

            return  "redirect:/list";
        }
        return "../login";
    }

    /**
     * 用户登录
     * @param model
     * @param email
     * @param password
     * @param code
     * @param telephone
     * @param phone_code
     * @param state
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/doLogin")
    public String doLogin(Model model, @RequestParam(value = "username",required = false) String email,
                          @RequestParam(value = "password",required = false) String password,
                          @RequestParam(value = "code",required = false) String code,
                          @RequestParam(value = "telephone",required = false) String telephone,
                          @RequestParam(value = "phone_code",required = false) String phone_code,
                          @RequestParam(value = "state",required = false) String state,
                          @RequestParam(value = "pageNum",required = false) Integer pageNum ,
                          @RequestParam(value = "pageSize",required = false) Integer pageSize) {

        //判断是否是手机登录
        if (StringUtils.isNotBlank(telephone)) {
            //手机登录
            String yzm = redisTemplate.opsForValue().get( telephone );//从redis获取验证码
            if(phone_code.equals(yzm)){
                //验证码正确
                User user = userService.findByPhone(telephone);
                getSession().setAttribute("user", user);
                model.addAttribute("user", user);
                log.info("手机快捷登录成功");
                return "redirect:/list";

            }else {
                //验证码错误或过期
                model.addAttribute("error","phone_fail");
                return  "../login";
            }

        } else {
            //账号登录

        if (StringUtils.isBlank(code)) {
            model.addAttribute("error", "fail");
            return "../login";
        }
        int b = checkValidateCode(code);
        if (b == -1) {
            model.addAttribute("error", "fail");
            return "../login";
        } else if (b == 0) {
            model.addAttribute("error", "fail");
            return "../login";
        }
        password = MD5Util.encodeToHex(Constants.SALT + password);
        User user = userService.login(email, password);
        if (user != null) {
            if ("0".equals(user.getState())) {
                //未激活
                model.addAttribute("email", email);
                model.addAttribute("error", "active");
                return "../login";
            }
            log.info("用户登录登录成功");
            getSession().setAttribute("user", user);
            model.addAttribute("user", user);
            return "redirect:/list";
        } else {
            log.info("用户登录登录失败");
            model.addAttribute("email", email);
            model.addAttribute("error", "fail");
            return "../login";
        }

    }

    }

    // 匹对验证码的正确性
    public int checkValidateCode(String code) {
        Object vercode = getRequest().getSession().getAttribute("VERCODE_KEY");
        if (null == vercode) {
            return -1;
        }
        if (!code.equalsIgnoreCase(vercode.toString())) {
            return 0;
        }
        return 1;
    }

    /**
     * 发送手机验证码
     * @param model
     * @param telephone
     * @return
     */
    @RequestMapping("/sendSms")
    @ResponseBody
    public Map<String,Object> index(Model model, @RequestParam(value = "telephone",required = false) final String telephone ) {
        Map map = new HashMap<String,Object>(  );
        try { //  发送验证码操作
            final String code = RandStringUtils.getCode();
            redisTemplate.opsForValue().set(telephone, code, 60, TimeUnit.SECONDS);// 60秒 有效 redis保存验证码
            log.debug("--------短信验证码为："+code);
            // 调用ActiveMQ jmsTemplate，发送一条消息给MQ
            jmsTemplate.send("login_msg", new MessageCreator() {
                public Message createMessage(javax.jms.Session session) throws JMSException {
                    MapMessage mapMessage = session.createMapMessage();
                    mapMessage.setString("telephone",telephone);
                    mapMessage.setString("code", code);
                    return mapMessage;
                }
            });
        } catch (Exception e) {
            map.put( "msg",false );
        }
        map.put( "msg",true );
        return map;

    }

    /**
     * 退出登录
     * @param model
     * @return
     */
    @RequestMapping("/loginout")
    public String exit(Model model) {
        log.info( "退出登录" );
        getSession().removeAttribute( "user" );
        getSession().invalidate();
        return "../login";
    }


}

package com.dnnt.touch.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dnnt.touch.domain.Json;
import com.dnnt.touch.mapper.UserMapper;
import com.dnnt.touch.util.Secure;
import com.dnnt.touch.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dnnt.touch.domain.User;
import org.springframework.web.servlet.ModelAndView;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController{

    private static final String TOKEN_KEY = "!r,,zGLkX7T^Rs60+tQYssiThTRFn@IZ(|NUy599aD[f>R`=DK.rM@X[VJOgiho";
    private Algorithm hmacSHA = Algorithm.HMAC256(TOKEN_KEY);

    @Autowired
    private UserMapper userMapper;

    public UserController() throws UnsupportedEncodingException {
    }

    @RequestMapping("/login")
    public Json<User> login(String nameOrPhone , String password) {
        //查找用户是否存在
        User user = userMapper.selectByName(nameOrPhone);
        if (user == null){
            user = userMapper.selectByPhone(nameOrPhone);
            if (user == null){
                return generateFailure("用户名/手机错误");
            }
        }

        Json<User> data;
        String md5Pwd = Secure.getMD5Hex(password.getBytes());
        if (md5Pwd.equals(user.getPassword())){
            user.setPassword("");
            //生成并设置token,一星期后过期
            String token = JWT.create()
                    .withExpiresAt(new Date(System.currentTimeMillis() + 7*24*60*60*1000))
                    .withClaim(User.USER_NAME,user.getUserName())
                    .withClaim(User.PHONE,user.getPhone())
                    .withClaim(User.ID,user.getId())
                    .withClaim(User.HEAD_URL,user.getHeadUrl())
                    .sign(hmacSHA);
            user.setToken(token);
            data = generateSuccessful(user);
        }else {
            data = generateFailure("用户名或密码错误!");
        }
        return data;
    }

    @RequestMapping("/register")
    public Json<Void> register(String userName, String password, String phone, String verificationCode){
        //verificationCode
        Object code = SessionUtil.getAttr(phone);
        if (code == null || !code.equals(verificationCode)){
            return generateFailure("非法注册!!!");
        }
        if (userName.length() < 4 || userName.length() > 16){
            return generateFailure("用户名至少为4位，最多为16位");
        }
        if (password.length() < 6){
            return generateFailure("密码至少为6位");
        }

        Json<Void> data;
        User user = userMapper.selectByName(userName);
        if (user == null){
            String md5Pwd = Secure.getMD5Hex(password.getBytes());
            user = new User();
            user.setUserName(userName);
            user.setPassword(md5Pwd);
            user.setPhone(phone);
            userMapper.insertUser(user);
            data = generateSuccessful(null);
        }else {
            data = generateFailure("用户名已存在!");
        }
        return data;
    }

    @RequestMapping("/getVerificationCode")
    public Json<Void> getVerificationCode(String phone){
        if (phone.length() != 11){
            return generateFailure("手机格式不正确");
        }
        Json<Void> data;
        if (userMapper.selectByPhone(phone) == null){
            Random random = new Random(System.currentTimeMillis());
            String code = String.valueOf(random.nextInt(1000000));
            while (code.length() < 6){
                code = "0" + code;
            }
            //TODO 发送手机验证码
            SessionUtil.setAttr(phone,code);
            System.out.println(phone + " : " + code);
            data = generateSuccessful(null);
        }else {
            data = generateFailure("该手机号已注册");
        }
        return data;
    }

    @RequestMapping("/codeVerification")
    public Json<Void> codeVerification(String phone, String verificationCode){
        Object code = SessionUtil.getAttr(phone);
        if (code == null){
            return generateFailure("验证码过期或未获取验证码");
        }
        System.out.println(code);
        Json<Void> data;
        if (code.equals(verificationCode)){
            data = generateSuccessful(null);
        }else {
            data = generateFailure("验证码错误");
        }
        return data;
    }

    @RequestMapping("/test")
    public Json<Void> test(){
        return generateSuccessful(null);
    }
}

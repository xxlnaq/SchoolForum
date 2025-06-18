package com.example.controller;


import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.dto.AccountDetails;
import com.example.entity.vo.request.ChangePasswordVo;
import com.example.entity.vo.request.DetailsSaveVO;
import com.example.entity.vo.request.ModifyEmailVO;
import com.example.entity.vo.request.PrivacySaveVO;
import com.example.entity.vo.response.AccountDetailsVO;
import com.example.entity.vo.response.AccountPrivacyVO;
import com.example.entity.vo.response.AccountVO;
import com.example.service.AccountDetailsService;
import com.example.service.AccountPrivacyService;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    ControllerUtils utils;

    @Resource
    AccountPrivacyService privacyService;

    @Resource
    AccountService accountService;

    @Resource
    AccountDetailsService detailsService;

    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int id) {
       Account account= accountService.findAccountById(id);
       return RestBean.success(account.asViewObject(AccountVO.class));
    }

    //用户查询接口
    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        AccountDetails details= Optional
                .ofNullable(detailsService.findAccountDetailsById(id))
                .orElse(new AccountDetails());
        return RestBean.success(details.asViewObject(AccountDetailsVO.class));
    }

    @PostMapping("/save-details")
    public RestBean<Void> save(@RequestAttribute(Const.ATTR_USER_ID) int id,
                               @RequestBody @Validated DetailsSaveVO vo) {
        Boolean  bool = detailsService.saveAccountDetails(id, vo);
        return  bool?  RestBean.success():RestBean.failure(400,"此用户名已经被其他用户注册");
    }
    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id, @RequestBody @Valid ModifyEmailVO vo) {
      String result=accountService.modifyEmail(id, vo);
      return result==null ? RestBean.success():RestBean.failure(400,result);
    }
    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVo vo){
//                String result=accountService.changePassword(id, vo);
//                return result==null ? RestBean.success():RestBean.failure(400,result);
        return this.messageHandle(()->accountService.changePassword(id, vo));
    }
    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid PrivacySaveVO vo){
   privacyService.savePrivacy(id, vo);
   return RestBean.success();

    }
    @GetMapping("/privacy")
   public RestBean<AccountPrivacyVO> privacy(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(privacyService.accessPrivacy(id).asViewObject(AccountPrivacyVO.class));
    }

    /**
     * 针对于返回值为String作为错误信息的方法进行统一处理
     * @param action 具体操作
     * @return 响应结果
     * @param <T> 响应结果类型
     */
    private <T> RestBean<T> messageHandle(Supplier<String> action){
        String message = action.get();
        if(message == null)
            return RestBean.success();
        else
            return RestBean.failure(400, message);
    }
}

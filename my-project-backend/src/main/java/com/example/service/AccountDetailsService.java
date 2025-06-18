package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.AccountDetails;
import com.example.entity.vo.request.DetailsSaveVO;

public interface AccountDetailsService extends IService<AccountDetails> {
    AccountDetails findAccountDetailsById(Integer id);
    Boolean saveAccountDetails(int id, DetailsSaveVO vo);
}

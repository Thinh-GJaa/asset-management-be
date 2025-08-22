package com.concentrix.asset.mapper.helper;

import com.concentrix.asset.entity.Account;
import com.concentrix.asset.entity.Site;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.AccountRepository;
import com.concentrix.asset.repository.SiteRepository;
import com.concentrix.asset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class IdMapperHelper {

    AccountRepository accountRepository;
    SiteRepository siteRepository;
    UserRepository userRepository;

    @Named("siteIdToSite")
    public Site siteIdToSite(Integer siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.SITE_NOT_FOUND, siteId));
    }

    @Named("accountIdToAccount")
    public Account getAccountById(Integer accountId) {
        if (accountId == null) {
            return null;
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND, accountId));
    }

    @Named("userEidToUser")
    public User userEidToUser(String userEid) {
        if (userEid == null || userEid.isBlank())
            return null;

        return userRepository.findById(userEid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, userEid));
    }

    @Named("msaClientToAccount")
    public Account msaClientToAccount(String msaClient) {
        if (msaClient == null || msaClient.isBlank())
            return null;

        log.info("[msaClientToAccount] Fetching account for MSA client: {}", msaClient);

        return accountRepository.findByAccountName(msaClient)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NAME_NOT_FOUND, msaClient));
    }


}

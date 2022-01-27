package org.jetlinks.community.standalone.authorize;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.events.AuthorizationSuccessEvent;
import org.jetlinks.community.auth.service.MenuGrantService;
import org.jetlinks.community.auth.service.UserDetailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Lind
 * @since 1.0.0
 */
@Component
public class LoginEvent {
    private final UserDetailService detailService;

    private final MenuGrantService menuGrantService;

    public LoginEvent(UserDetailService detailService, MenuGrantService menuGrantService) {
        this.detailService = detailService;
        this.menuGrantService = menuGrantService;
    }

    @EventListener
    public void handleLoginSuccess(AuthorizationSuccessEvent event) {
        Map<String, Object> result = event.getResult();
        Authentication authentication = event.getAuthentication();
        List<Dimension> dimensions = authentication.getDimensions();

        result.put("dimensions", dimensions);
        result.put("permissions", authentication.getPermissions());
        result.put("roles", dimensions.stream().filter(dimension -> dimension.typeIs("role")).collect(Collectors.toList()));
        result.put("currentAuthority", authentication.getPermissions().stream().map(Permission::getId).collect(Collectors.toList()));

        event.async(
            detailService
                .findUserDetail(event.getAuthentication().getUser().getId())
                .doOnNext(detail -> result.put("user", detail))
        );

        event.async(
            menuGrantService.getGrantMenuList(authentication)
                .as(menuGrantService::listToTree)
                .collect(Collectors.toList())
                .doOnNext(menus -> result.put("menus", menus))
        );
    }
}

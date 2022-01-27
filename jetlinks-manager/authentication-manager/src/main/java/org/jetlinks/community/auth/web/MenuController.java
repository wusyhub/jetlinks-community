package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.enums.MenuStateEnum;
import org.jetlinks.community.auth.service.AuthorizationSettingDetailService;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.service.MenuGrantService;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单管理
 *
 * @author wangzheng
 * @since 1.0
 */
@RestController
@RequestMapping("/menu")
@Authorize
@Resource(id = "menu", name = "菜单管理", group = "system")
@Tag(name = "菜单管理")
@AllArgsConstructor
public class MenuController implements ReactiveServiceCrudController<MenuEntity, String> {

    private final DefaultMenuService defaultMenuService;

    private final MenuGrantService menuGrantService;

    private final AuthorizationSettingDetailService settingService;

    @Override
    public ReactiveCrudService<MenuEntity, String> getService() {
        return defaultMenuService;
    }

    @Override
    @PatchMapping
    @SaveAction
    @Operation(summary = "保存数据", description = "如果传入了id,并且对应数据存在,则尝试覆盖,不存在则新增.")
    public Mono<SaveResult> save(@RequestBody Flux<MenuEntity> payload) {
        return Authentication.currentReactive()
            .flatMapMany(auth -> payload.map(entity -> applyAuthentication(entity, auth)))
            .switchIfEmpty(payload)
            .flatMap(entity -> getService().createQuery()
                .where(MenuEntity::getPermission, entity.getPermission())
                .fetchOne().map(room -> {
                    if (room.getId().equals(entity.getId())) {
                        return entity;
                    }
                    throw new RuntimeException("权限标识已存在!");
                }).defaultIfEmpty(entity))
            .as(getService()::save);
    }


    /**
     * 获取用户自己的菜单列表
     *
     * @return 菜单列表
     */
    @GetMapping("/user-own/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(树结构)")
    public Flux<MenuEntity> getUserMenuAsTree() {
        return this.getUserMenuAsList().as(menuGrantService::listToTree);
    }


    @GetMapping("/user-own/list")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(列表结构)")
    public Flux<MenuEntity> getUserMenuAsList() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(autz -> defaultMenuService
                .createQuery()
                //.where(MenuEntity::getStatus, MenuStateEnum.ENABLED.getValue())
                .fetch()
                .collect(Collectors.toMap(MenuEntity::getId, Function.identity()))
                .flatMapIterable(menuMap ->
                    menuGrantService.convert(menuMap,
                        menu -> "admin".equals(autz.getUser().getUsername()) ||
                            menu.hasPermission(autz::hasPermission)
                    ))
            );
    }

    @GetMapping("/role-own/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取用户角色的菜单列表(树结构)")
    public Flux<MenuEntity> getUserRoleMenuAsTree() {
        return this.getUserRoleMenuAsList().as(menuGrantService::listToTree);
    }


    @GetMapping("/role-own/list")
    @Authorize(merge = false)
    @Operation(summary = "获取用户角色的菜单列表(列表结构)")
    public Flux<MenuEntity> getUserRoleMenuAsList() {
        return menuGrantService.getUserRoleMenuAsList();
    }

    @PutMapping("/_grant")
    @Operation(summary = "根据菜单进行授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> grant(@RequestBody Mono<MenuGrantRequest> body) {
        return body
            .map(request -> menuGrantService.grant(request).thenReturn(request))
            .flatMap(request -> Mono.zip(request,
                defaultMenuService
                    .createQuery()
                    .where(MenuEntity::getStatus, MenuStateEnum.ENABLED.getValue())
                    .fetch()
                    .collectList(),
                MenuGrantRequest::toAuthorizationSettingDetail
            ))
            .filter(detail -> CollectionUtils.isNotEmpty(detail.getPermissionList()))
            .flatMap(detail -> Authentication.currentReactive()
                .map(auth -> settingService.saveDetail(auth, Flux.just(detail)))
            ).flatMap(Function.identity());
    }

    @GetMapping("/{targetType}/{targetId}/_grant/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取根据授权类型菜单授权信息(树结构)")
    public Flux<MenuEntity> getGrantInfoTree(@PathVariable String targetType,
                                             @PathVariable String targetId) {

        return this.getGrantTargetInfo(targetType, targetId).as(menuGrantService::listToTree);
    }

    @GetMapping("/{targetType}/{targetId}/_grant/list")
    @Authorize(merge = false)
    @Operation(summary = "获取根据授权类型菜单授权信息(列表结构)")
    public Flux<MenuEntity> getGrantTargetInfo(@PathVariable String targetType, @PathVariable String targetId) {
        return menuGrantService.getGrantTargetInfo(targetType, targetId);
    }

    @GetMapping("/_grant/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取菜单授权信息(树结构)")
    public Flux<MenuEntity> getGrantInfoTree() {
        return this.getGrantInfo().as(menuGrantService::listToTree);
    }

    @GetMapping("/_grant/list")
    @Authorize(merge = false)
    @Operation(summary = "获取菜单授权信息(列表结构)")
    public Flux<MenuEntity> getGrantInfo() {
        return menuGrantService.getGrantMenuList();
    }

}

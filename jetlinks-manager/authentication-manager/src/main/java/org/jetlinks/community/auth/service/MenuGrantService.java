package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuGrantEntity;
import org.jetlinks.community.auth.enums.MenuStateEnum;
import org.jetlinks.community.auth.enums.MenuTypeEnum;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/19 下午4:52
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
@Service
public class MenuGrantService extends GenericReactiveCrudService<MenuGrantEntity, String> {

    private final String ROLE_DIMENSION_TYPE_ID = "role";

    private final String USER_DIMENSION_TYPE_ID = "user";

    @Autowired
    private DefaultMenuService defaultMenuService;

    public Mono<SaveResult> grant(MenuGrantRequest body) {
        //先删除之前的授权情况,再重新进行授权
        return this.createDelete()
            .where(MenuGrantEntity::getTargetId, body.getTargetId())
            .where(MenuGrantEntity::getTargetType, body.getTargetType())
            .execute().thenReturn(body)
            .flatMap(request -> this.save(Flux.fromIterable(request.getMenuIds().stream().map(menuId -> {
                MenuGrantEntity grant = new MenuGrantEntity();
                grant.setMenuId(menuId);
                grant.setTargetId(request.getTargetId());
                grant.setTargetType(request.getTargetType());
                return grant;
            }).collect(Collectors.toList()))));
    }

    /**
     * 获取授权信息
     *
     * @return
     */
    public Flux<MenuGrantEntity> getGrantInfo(String targetType, String targetId) {
        return this.createQuery().where(MenuGrantEntity::getTargetType, targetType)
            .where(MenuGrantEntity::getTargetId, targetId).fetch();
    }

    /**
     * 获取根据授权类型菜单授权信息
     *
     * @param targetType
     * @param targetId
     * @return
     */
    public Flux<MenuEntity> getGrantTargetInfo(String targetType, String targetId) {
        return Authentication.currentReactive().switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(target -> this.getGrantInfo(targetType, targetId))
            .map(MenuGrantEntity::getMenuId)
            .collect(Collectors.toSet())
            .flatMapMany(list -> {
                if (org.apache.commons.collections4.CollectionUtils.isEmpty(list)) {
                    return Flux.empty();
                }
                return defaultMenuService
                    .createQuery()
                    .where(MenuEntity::getStatus, MenuStateEnum.ENABLED.getValue())
                    .in(MenuEntity::getId, list)
                    .fetch();
            });
    }

    /**
     * 用户菜单id和角色菜单id进行交集
     *
     * @param userMenuIds
     * @param roleMenuIds
     * @return
     */
    public Set<String> intersection(Set<String> userMenuIds, Set<String> roleMenuIds) {
        //用户角色未分配权限
        if (CollectionUtils.isEmpty(roleMenuIds)) {
            return new HashSet<>();
        }
        //用户未分配权限
        if (CollectionUtils.isEmpty(userMenuIds)) {
            return roleMenuIds;
        }
        return userMenuIds.stream().filter(menuIds -> roleMenuIds.contains(menuIds)).collect(Collectors.toSet());
    }


    /**
     * 获取用户所有角色授权菜单
     *
     * @return
     */
    public Flux<MenuEntity> getUserRoleMenuAsList() {
        return Authentication.currentReactive().switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> {
                List<MenuGrantRequest> targets = new ArrayList<>();
                auth.getDimensions(ROLE_DIMENSION_TYPE_ID).forEach(dimension -> {
                    targets.add(new MenuGrantRequest(ROLE_DIMENSION_TYPE_ID, dimension.getId()));
                });
                return Flux.fromIterable(targets);
            })
            .flatMap(target -> this.getGrantInfo(target.getTargetType(), target.getTargetId()))
            .map(MenuGrantEntity::getMenuId)
            .collect(Collectors.toSet())
            .flatMapMany(list -> {
                if (org.apache.commons.collections4.CollectionUtils.isEmpty(list)) {
                    return Flux.empty();
                }
                return defaultMenuService
                    .createQuery()
                    .where(MenuEntity::getStatus, MenuStateEnum.ENABLED.getValue())
                    .in(MenuEntity::getId, list)
                    .fetch();
            });
    }


    /**
     * 获取最终用户菜单授权情况
     *
     * @return
     */
    public Flux<MenuEntity> getGrantMenuList() {
        return Authentication.currentReactive().switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(authentication -> this.getGrantMenuList(authentication));
    }

    /**
     * 获取最终用户菜单授权情况
     *
     * @return
     */
    public Flux<MenuEntity> getGrantMenuList(Authentication authentication) {
        List<MenuGrantRequest> targets = new ArrayList<>();
        authentication.getDimensions(ROLE_DIMENSION_TYPE_ID).forEach(dimension -> {
            targets.add(new MenuGrantRequest(ROLE_DIMENSION_TYPE_ID, dimension.getId()));
        });
        return Mono.zip(
                //先获取用户分配的权限
                this.getGrantInfo(USER_DIMENSION_TYPE_ID, authentication.getUser().getId())
                    .map(MenuGrantEntity::getMenuId)
                    .collect(Collectors.toSet()),
                //先获取用户角色分配的权限
                Flux.fromIterable(targets)
                    .flatMap(target -> this.getGrantInfo(target.getTargetType(), target.getTargetId()))
                    .map(MenuGrantEntity::getMenuId)
                    .collect(Collectors.toSet()),
                //用户菜单id和角色菜单id进行交集
                this::intersection
            )
            .flatMapMany(list -> {
                if (org.apache.commons.collections4.CollectionUtils.isEmpty(list)) {
                    return Flux.empty();
                }
                return defaultMenuService
                    .createQuery()
                    .where(MenuEntity::getStatus, MenuStateEnum.ENABLED.getValue())
                    .in(MenuEntity::getId, list)
                    .fetch();
            });
    }


    /**
     * 过滤权限
     *
     * @param menuMap
     * @param predicate
     * @return
     */
    public Collection<MenuEntity> convert(Map<String, MenuEntity> menuMap, Predicate<MenuEntity> predicate) {
        Map<String, MenuEntity> group = new HashMap<>(8);
        for (MenuEntity menu : menuMap.values()) {
            if (group.containsKey(menu.getId())) {
                continue;
            }
            if (predicate.test(menu)) {
                String parentId = menu.getParentId();
                MenuEntity parent;
                group.put(menu.getId(), menu);
                //有子菜单默认就有父菜单
                while (!StringUtils.isEmpty(parentId)) {
                    parent = menuMap.get(parentId);
                    if (parent == null) {
                        break;
                    }
                    parentId = parent.getParentId();
                    group.put(parent.getId(), parent);
                }
            }
        }
        return group.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * 构建树结构
     *
     * @param flux
     * @return
     */
    public Flux<MenuEntity> listToTree(Flux<MenuEntity> flux) {
        return flux.collectList().flatMapIterable(list ->
                TreeSupportEntity.list2tree(list,
                    MenuEntity::setChildren,
                    (Predicate<MenuEntity>) n ->
                        StringUtils.isEmpty(n.getParentId()) || "-1".equals(n.getParentId()))
            ).map(menu -> this.subordinate(menu))
            .sort(Comparator.comparing(MenuEntity::getSortIndex));
    }

    /**
     * 查询子菜单和按钮组
     *
     * @param menu
     */
    public MenuEntity subordinate(MenuEntity menu) {
        //没有子菜单和按钮
        if (menu.getChildren() == null) {
            return menu;
        }
        List<MenuEntity> children = new ArrayList<>();
        List<MenuEntity> buttons = new ArrayList<>();
        for (MenuEntity entity : menu.getChildren()) {
            //查找按钮组
            if (MenuTypeEnum.BUTTON.getValue().equals(entity.getType())) {
                buttons.add(entity);
                //continue;
            }
            children.add(entity);
            //子菜单继续查找下级菜单
            this.subordinate(entity);
        }
        menu.setChildren(children
            .stream()
            .sorted(Comparator.comparing(MenuEntity::getSortIndex))
            .collect(Collectors.toList()));
        menu.setButtons(buttons);
        return menu;
    }
}

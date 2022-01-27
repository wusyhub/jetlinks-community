package org.jetlinks.community.auth.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.PermissionInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuGrantRequest {


    @Schema(description = "类型")
    private String targetType;

    @Schema(description = "类型id")
    private String targetId;
    /**
     * 冲突时是否合并
     */
    @Schema(description = "冲突时是否合并")
    private boolean merge = true;

    /**
     * 冲突时优先级
     */
    @Schema(description = "冲突时合并优先级")
    private int priority = 10;

    @Schema(description = "授权的菜单信息")
    private List<String> menuIds;

    public MenuGrantRequest(String targetType, String targetId) {
        this.targetType = targetType;
        this.targetId = targetId;
    }

    /**
     * 获取接口权限信息
     *
     * @param list
     * @return
     */
    public AuthorizationSettingDetail toAuthorizationSettingDetail(List<MenuEntity> list) {
        Map<String, MenuEntity> menuMap = list
            .stream()
            .collect(Collectors.toMap(MenuEntity::getId, Function.identity()));
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType(targetType);
        detail.setTargetId(targetId);
        detail.setMerge(merge);
        detail.setPriority(priority);
        Map<String, Set<String>> permissions = new ConcurrentHashMap<>(8);
        for (String menuId : menuIds) {
            MenuEntity entity = menuMap.get(menuId);
            if (entity == null) {
                continue;
            }
            //自动持有配置的权限
            if (CollectionUtils.isNotEmpty(entity.getPermissions())) {
                for (PermissionInfo permission : entity.getPermissions()) {
                    permissions
                        .computeIfAbsent(permission.getPermission(), ignore -> new HashSet<>())
                        .addAll(permission.getActions());
                }
            }
        }
        detail.setPermissionList(permissions
            .entrySet()
            .stream()
            .map(e -> AuthorizationSettingDetail.PermissionInfo.of(e.getKey(), e.getValue()))
            .collect(Collectors.toList()));
        return detail;
    }


}

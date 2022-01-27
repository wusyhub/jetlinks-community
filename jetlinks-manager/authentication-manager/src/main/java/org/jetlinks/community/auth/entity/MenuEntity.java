package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * 菜单定义实体类
 *
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "s_menu", indexes = {
    @Index(name = "idx_menu_path", columnList = "path")
})
public class MenuEntity extends GenericTreeSortSupportEntity<String> {

    @Schema(description = "名称")
    @Comment("菜单名称")
    @Column(length = 32)
    private String name;

    @Schema(description = "分类1-系统 2-租户")
    @Comment("分类1-系统 2-租户")
    @Column(name = "category", length = 1)
    @DefaultValue("1")
    private Integer category;

    @Schema(description = "租户id")
    @Comment("菜单名称")
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Schema(description = "类型1-目录 2-菜单 3-按钮")
    @Comment("类型1-目录 2-菜单 3-按钮")
    @Column(name = "type", length = 1)
    @DefaultValue("2")
    private Integer type;

    @Comment("描述")
    @Column(length = 256)
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "描述")
    private String describe;

    @Schema(description = "权限标识")
    @Comment("权限标识")
    @Column(length = 64)
    private String permission;

    @Comment("URL,路由")
    @Column(length = 512)
    @Schema(description = "URL,路由")
    private String url;

    @Comment("组件路径")
    @Column(length = 256)
    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "打开方式")
    @Comment("打开方式")
    @Column(name = "target", length = 256)
    private String target;

    @Comment("图标")
    @Column(length = 256)
    @Schema(description = "图标")
    private String icon;

    @Comment("状态")
    @Column(length = 1)
    @ColumnType(jdbcType = JDBCType.SMALLINT)
    @Schema(description = "状态,0为禁用,1为启用")
    @DefaultValue("1")
    private Integer status;

    @Schema(description = "默认权限信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private List<PermissionInfo> permissions;


    @Schema(description = "其他配置信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Object> options;

    @Schema(description = "按钮定义信息")
    private List<MenuEntity> buttons;

    @Schema(description = "子菜单")
    private List<MenuEntity> children;

    /**
     * 判断是否有权限
     *
     * @param predicate
     * @return
     */
    public boolean hasPermission(BiPredicate<String, Collection<String>> predicate) {
        //没有配置权限信息说明,该菜单所有人都可以访问
        if (CollectionUtils.isEmpty(permissions)) {
            return true;
        }
        //有权限信息
        if (CollectionUtils.isNotEmpty(permissions)) {
            for (PermissionInfo permission : permissions) {
                if (!predicate.test(permission.getPermission(), permission.getActions())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

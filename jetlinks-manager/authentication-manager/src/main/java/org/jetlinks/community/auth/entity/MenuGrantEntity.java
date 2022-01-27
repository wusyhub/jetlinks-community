package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/19 下午3:53
 * Description :菜单授权信息表
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
@Getter
@Setter
@Table(name = "s_menu_grant")
public class MenuGrantEntity extends GenericEntity<String> {

    /**
     * 设置目标类型(维度)标识,如: role , user
     */
    @Column(length = 32)
    @Schema(description = "权限类型,如: org,user")
    private String targetType;

    /**
     * 设置目标.
     */
    @Column(length = 64)
    @Schema(description = "权限类型对应的数据ID")
    private String targetId;

    /**
     * 菜单Id.
     */
    @Column(length = 64)
    @Schema(description = "菜单Id")
    private String menuId;

}

package org.jetlinks.community.auth.enums;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/19 下午4:52
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
public enum MenuTypeEnum {

    /**
     * 菜单
     */
    CATALOG(1),

    /**
     * 菜单
     */
    MENU(2),

    /**
     * 按钮
     */
    BUTTON(3);

    private final Integer value;

    MenuTypeEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}

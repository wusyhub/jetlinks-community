package org.jetlinks.community.auth.enums;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/19 下午4:52
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
public enum MenuStateEnum {

    /**
     * 启用
     */
    ENABLED(1),

    /**
     * 禁用
     */
    DISABLED(0);


    private final Integer value;

    MenuStateEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}

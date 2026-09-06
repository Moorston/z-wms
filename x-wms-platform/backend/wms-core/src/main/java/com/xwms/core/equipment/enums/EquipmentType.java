package com.xwms.core.equipment.enums;

import lombok.Getter;

/** 设备类型 */
@Getter
public enum EquipmentType {
    FORKLIFT("FORKLIFT", "叉车"),
    PALLET_JACK("PALLET_JACK", "托盘车"),
    CONVEYOR("CONVEYOR", "输送线"),
    SORTER("SORTER", "分拣机"),
    AGV("AGV", "AGV小车"),
    RFID_READER("RFID_READER", "RFID读写器"),
    BARCODE_SCANNER("BARCODE_SCANNER", "扫码枪"),
    PRINTER("PRINTER", "打印机"),
    WEIGHING_SCALE("WEIGHING_SCALE", "电子秤"),
    PDA("PDA", "手持终端"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    EquipmentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

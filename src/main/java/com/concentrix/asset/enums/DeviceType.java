package com.concentrix.asset.enums;

public enum DeviceType {
    // Devices có serial
    MONITOR(true),
    DESKTOP(true),
    LAPTOP(true),
    IMAC(true),
    MACBOOK(true),
    MAC_MINI(true),
    GRINGOTTS(true),
    SFP(true),
    SWITCH(true),
    FIREWALL(true),
    ROUTER(true),
    AP(true),
    SERVER(true),
    IPHONE(true),
    CHROME_BOOK(true),
    POWER_MODULE(true),

    // Accessories không có serial
    ADAPTER(false),
    BATTERY(false),
    BOX(false),
    CABLE(false),
    CAMERA(false),
    CONVERTER(false),
    DOCK(false),
    DONGLE(false),
    HEADSET(false),
    HUB(false),
    INK(false),
    JAPANESE_KEYBOARD(false),
    KEYBOARD(false),
    MEMORY(false),
    MOUSE(false),
    NETWORK(false),
    OFFICE_SUPPLIER(false),
    OTHER(false),
    PHONE(false),
    PRINTER(false),
    STACKING_CABLE(false),
    TOKEN(false),
    UBIKEY(false),
    USB(false),
    SPEAKER(false),
    USB_WIFI(false),
    VOICE(false),
    SERVER_HDD(false);

    private final boolean hasSerial;

    DeviceType(boolean hasSerial) {
        this.hasSerial = hasSerial;
    }

    public boolean hasSerial() {
        return hasSerial;
    }
}

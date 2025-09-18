package com.concentrix.asset.service;

import com.concentrix.asset.enums.DeviceType;
import com.concentrix.asset.service.impl.TypeServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypeServiceTest {

    private final TypeServiceImpl service = new TypeServiceImpl();

    @Test
    void getTypes_returnsAllEnumValues() {
        List<DeviceType> types = service.getTypes();
        assertNotNull(types);
        assertEquals(DeviceType.values().length, types.size());
        // spot check contains a few known values
        assertTrue(types.contains(DeviceType.LAPTOP));
        assertTrue(types.contains(DeviceType.MOUSE));
    }

    @Test
    void getTypeWithSerial_filtersByHasSerialTrue() {
        List<DeviceType> withSerial = service.getTypeWithSerial();
        assertNotNull(withSerial);
        // none should have hasSerial == false
        assertTrue(withSerial.stream().allMatch(DeviceType::hasSerial));
        // and must include at least one known true type, exclude a known false type
        assertTrue(withSerial.contains(DeviceType.LAPTOP));
        assertFalse(withSerial.contains(DeviceType.MOUSE));

        // Validate partition size math against enum definition
        long expectedTrue = EnumSet.allOf(DeviceType.class).stream().filter(DeviceType::hasSerial).count();
        assertEquals(expectedTrue, withSerial.size());
    }

    @Test
    void getTypeWithoutSerial_filtersByHasSerialFalse() {
        List<DeviceType> withoutSerial = service.getTypeWithoutSerial();
        assertNotNull(withoutSerial);
        // none should have hasSerial == true
        assertTrue(withoutSerial.stream().noneMatch(DeviceType::hasSerial));
        // include a known false type, exclude a known true type
        assertTrue(withoutSerial.contains(DeviceType.MOUSE));
        assertFalse(withoutSerial.contains(DeviceType.LAPTOP));

        long expectedFalse = EnumSet.allOf(DeviceType.class).stream().filter(t -> !t.hasSerial()).count();
        assertEquals(expectedFalse, withoutSerial.size());

        // Verify the two sets are a partition of all types
        Set<DeviceType> union = EnumSet.copyOf(withoutSerial);
        union.addAll(service.getTypeWithSerial());
        assertEquals(EnumSet.allOf(DeviceType.class).size(), union.size());
    }
}



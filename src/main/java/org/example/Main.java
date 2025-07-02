package org.example;

import java.time.LocalDateTime;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        //  LocalDateTime.of(2000, 1, 1, 12, 0);
        Person person = new Person("Vasyn", 44,
                1.51, LocalDateTime.of(1547, 4, 15, 18, 53));
        OrmService ormService = new OrmService();
        ormService.create(person);

    }
}
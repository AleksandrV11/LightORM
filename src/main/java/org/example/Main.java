package org.example;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {

        Person person = new Person("Vetaly", 44,
                1.51, LocalDateTime.of(1547, 4, 15, 18, 53));
        OrmService ormService = new OrmService();
        // ormService.create(person);
        //  System.out.println(Optional.ofNullable(ormService.read(Person.class, 4L)));
        // ormService.update(person,2L);
        //  ormService.delete(person,4L);


    }
}
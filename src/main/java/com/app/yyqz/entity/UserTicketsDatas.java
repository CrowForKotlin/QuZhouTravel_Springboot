package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTicketsDatas {
    private int bookingTicketsCount;
    private String tourName;
    private String tourImageName;
    private String date;
}

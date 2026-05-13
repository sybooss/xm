package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleSolutionOption {
    private String type;
    private String title;
    private String detail;
    private String risk;
}

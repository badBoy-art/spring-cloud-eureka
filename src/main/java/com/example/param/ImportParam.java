package com.example.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author: badBoy
 * @create: 2025-07-02 10:14
 * @Description:
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImportParam {

    private String billnum;
    private Object externalData;

}

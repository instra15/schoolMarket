package com.schoolMarket.common;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> {

    private T data;

    private boolean error;

    private String msg;

    public static <K> Response<K> success(K data,String msg)
    {
        Response<K> response=new Response<>();
        response.setData(data);
        response.setError(true);
        response.setMsg(msg);
        return response;
    }

    public static <K> Response<K> fail(Exception e)
    {
        Response<K> response=new Response<>();
        response.setError(false);
        response.setMsg(e.getMessage());
        return response;
    }

}

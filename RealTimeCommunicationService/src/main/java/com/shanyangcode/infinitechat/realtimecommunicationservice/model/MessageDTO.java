package com.shanyangcode.infinitechat.realtimecommunicationservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonPropertyOrder({"type", "data"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDTO {
    private Integer type;

    private Object data;
}
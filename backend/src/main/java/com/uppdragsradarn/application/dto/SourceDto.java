package com.uppdragsradarn.application.dto;

import java.util.UUID;

import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.SourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDto {

  private UUID id;

  @NotBlank private String name;

  private String baseUrl;

  @NotNull private UUID sourceTypeId;

  private String sourceTypeName;

  private String configuration;

  private boolean active;

  public static SourceDto fromEntity(Source source) {
    return SourceDto.builder()
        .id(source.getId())
        .name(source.getName())
        .baseUrl(source.getBaseUrl())
        .sourceTypeId(source.getSourceType().getId())
        .sourceTypeName(source.getSourceType().getName())
        .configuration(source.getConfiguration())
        .active(source.isActive())
        .build();
  }

  public Source toEntity(SourceType sourceType) {
    return Source.builder()
        .id(id)
        .name(name)
        .baseUrl(baseUrl)
        .sourceType(sourceType)
        .configuration(configuration)
        .active(active)
        .build();
  }
}

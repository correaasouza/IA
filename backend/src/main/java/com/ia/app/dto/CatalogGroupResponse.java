package com.ia.app.dto;

import java.util.ArrayList;
import java.util.List;

public class CatalogGroupResponse {
  private Long id;
  private String nome;
  private Long parentId;
  private Integer nivel;
  private Integer ordem;
  private String path;
  private boolean ativo;
  private Long totalItems;
  private List<CatalogGroupResponse> children = new ArrayList<>();

  public CatalogGroupResponse() {}

  public CatalogGroupResponse(
      Long id,
      String nome,
      Long parentId,
      Integer nivel,
      Integer ordem,
      String path,
      boolean ativo,
      Long totalItems) {
    this.id = id;
    this.nome = nome;
    this.parentId = parentId;
    this.nivel = nivel;
    this.ordem = ordem;
    this.path = path;
    this.ativo = ativo;
    this.totalItems = totalItems;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Integer getNivel() {
    return nivel;
  }

  public void setNivel(Integer nivel) {
    this.nivel = nivel;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Long getTotalItems() {
    return totalItems;
  }

  public void setTotalItems(Long totalItems) {
    this.totalItems = totalItems;
  }

  public List<CatalogGroupResponse> getChildren() {
    return children;
  }

  public void setChildren(List<CatalogGroupResponse> children) {
    this.children = children;
  }
}

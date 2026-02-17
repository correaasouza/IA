package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.PapelPermissao;
import com.ia.app.repository.PapelPermissaoRepository;
import com.ia.app.repository.PapelRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PapelSeedService {

  private final PapelRepository papelRepository;
  private final PapelPermissaoRepository papelPermissaoRepository;

  public PapelSeedService(PapelRepository papelRepository, PapelPermissaoRepository papelPermissaoRepository) {
    this.papelRepository = papelRepository;
    this.papelPermissaoRepository = papelPermissaoRepository;
  }

  public void seedDefaults(Long tenantId) {
    Papel admin = papelRepository.findByTenantIdAndNome(tenantId, "ADMIN")
      .orElseGet(() -> {
        Papel p = new Papel();
        p.setTenantId(tenantId);
        p.setNome("ADMIN");
        p.setDescricao("Administrador do locatário");
        p.setAtivo(true);
        return papelRepository.save(p);
      });

    papelRepository.findByTenantIdAndNome(tenantId, "MASTER")
      .orElseGet(() -> {
        Papel p = new Papel();
        p.setTenantId(tenantId);
        p.setNome("MASTER");
        p.setDescricao("Master do sistema");
        p.setAtivo(true);
        return papelRepository.save(p);
      });

    List<String> perms = List.of(
      "CONFIG_EDITOR",
      "USUARIO_MANAGE",
      "PAPEL_MANAGE",
      "RELATORIO_VIEW",
      "ENTIDADE_EDIT",
      "MOVIMENTO_ESTOQUE_OPERAR"
    );
    List<String> existentes = papelPermissaoRepository.findAllByTenantIdAndPapelId(tenantId, admin.getId())
      .stream().map(PapelPermissao::getPermissaoCodigo).toList();
    for (String codigo : perms) {
      if (existentes.contains(codigo)) continue;
      PapelPermissao pp = new PapelPermissao();
      pp.setTenantId(tenantId);
      pp.setPapelId(admin.getId());
      pp.setPermissaoCodigo(codigo);
      papelPermissaoRepository.save(pp);
    }
  }
}



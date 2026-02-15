import { expect, test } from '@playwright/test';

type Empresa = { id: number; tipo: 'MATRIZ' | 'FILIAL'; razaoSocial: string; padrao?: boolean };
type Agrupador = { id: number; nome: string; ativo: boolean; empresas: Array<{ empresaId: number; nome: string }> };

test.describe('Agrupadores de Empresa', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      (window as any).__E2E_TEST__ = true;
      localStorage.setItem('tenantId', '1');
      localStorage.setItem('tenantRoles', JSON.stringify(['MASTER', 'ADMIN']));
    });
  });

  test('deve criar agrupador e impedir empresa duplicada em outro agrupador', async ({ page }) => {
    const empresas: Empresa[] = [
      { id: 1, tipo: 'MATRIZ', razaoSocial: 'Matriz Zeta' },
      { id: 2, tipo: 'FILIAL', razaoSocial: 'Filial Norte', padrao: true },
      { id: 3, tipo: 'FILIAL', razaoSocial: 'Filial Sul' }
    ];

    let nextAgrupadorId = 200;
    const agrupadores: Agrupador[] = [
      { id: 100, nome: 'Grupo A', ativo: true, empresas: [] },
      { id: 101, nome: 'Grupo B', ativo: true, empresas: [] }
    ];
    const empresaVinculadaPorGrupo = new Map<number, number>();

    await page.route('**/api/me', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'e2e-user',
          username: 'e2e',
          roles: [],
          tenantRoles: ['MASTER', 'ADMIN'],
          permissions: ['CONFIG_EDITOR', 'RELATORIO_VIEW', 'ENTIDADE_EDIT'],
          tenantId: '1'
        })
      });
    });

    await page.route('**/api/access-controls**', route => {
      const method = route.request().method();
      if (method === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
        return;
      }
      route.fulfill({ status: 204, body: '' });
    });

    await page.route('**/api/atalhos**', route => {
      const method = route.request().method();
      if (method === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
        return;
      }
      route.fulfill({ status: 204, body: '' });
    });

    await page.route('**/api/me/empresa-padrao**', route => {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ empresaId: 2 }) });
    });

    await page.route('**/api/empresas?**', route => {
      const url = new URL(route.request().url());
      const nome = (url.searchParams.get('nome') || '').toLowerCase().trim();
      const pageParam = Number(url.searchParams.get('page') || '0');
      const size = Number(url.searchParams.get('size') || '100');
      const filtered = empresas.filter(e => !nome || e.razaoSocial.toLowerCase().includes(nome));
      const start = pageParam * size;
      const content = filtered.slice(start, start + size);
      const totalPages = Math.max(1, Math.ceil(filtered.length / size));
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content,
          totalPages,
          totalElements: filtered.length,
          number: pageParam,
          size
        })
      });
    });

    await page.route('**/api/empresas/*', route => {
      const id = Number(route.request().url().split('/').pop() || '0');
      const empresa = empresas.find(e => e.id === id);
      if (!empresa) {
        route.fulfill({ status: 404, body: '' });
        return;
      }
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(empresa) });
    });

    await page.route('**/api/configuracoes/FORMULARIO/10/agrupadores-empresa**', route => {
      const method = route.request().method();
      const path = new URL(route.request().url()).pathname;
      const suffix = path.replace('/api/configuracoes/FORMULARIO/10/agrupadores-empresa', '');

      if (method === 'GET' && (suffix === '' || suffix === '/')) {
        // Keep list without bound companies to force API-conflict path in UI E2E.
        const projection = agrupadores.map(g => ({ ...g, empresas: [] }));
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(projection) });
        return;
      }

      if (method === 'POST' && (suffix === '' || suffix === '/')) {
        const body = route.request().postDataJSON() as { nome: string };
        const novo: Agrupador = { id: ++nextAgrupadorId, nome: body.nome, ativo: true, empresas: [] };
        agrupadores.push(novo);
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(novo) });
        return;
      }

      const renameMatch = suffix.match(/^\/(\d+)\/nome$/);
      if (method === 'PATCH' && renameMatch) {
        const agrupadorId = Number(renameMatch[1]);
        const body = route.request().postDataJSON() as { nome: string };
        const group = agrupadores.find(g => g.id === agrupadorId);
        if (!group) {
          route.fulfill({ status: 404, body: '' });
          return;
        }
        group.nome = body.nome;
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(group) });
        return;
      }

      const addEmpresaMatch = suffix.match(/^\/(\d+)\/empresas$/);
      if (method === 'POST' && addEmpresaMatch) {
        const agrupadorId = Number(addEmpresaMatch[1]);
        const body = route.request().postDataJSON() as { empresaId: number };
        const group = agrupadores.find(g => g.id === agrupadorId);
        if (!group) {
          route.fulfill({ status: 404, body: '' });
          return;
        }
        const usedBy = [...empresaVinculadaPorGrupo.entries()]
          .find(([empresaId, groupId]) => empresaId === body.empresaId && groupId !== agrupadorId);
        if (usedBy) {
          route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              title: 'Conflito de integridade',
              detail: 'Esta empresa ja esta vinculada a outro agrupador nesta configuracao.'
            })
          });
          return;
        }
        const empresa = empresas.find(e => e.id === body.empresaId);
        if (!empresa) {
          route.fulfill({ status: 404, body: '' });
          return;
        }
        empresaVinculadaPorGrupo.set(body.empresaId, agrupadorId);
        if (!group.empresas.some(e => e.empresaId === body.empresaId)) {
          group.empresas.push({ empresaId: body.empresaId, nome: empresa.razaoSocial });
        }
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(group) });
        return;
      }

      const removeEmpresaMatch = suffix.match(/^\/(\d+)\/empresas\/(\d+)$/);
      if (method === 'DELETE' && removeEmpresaMatch) {
        const agrupadorId = Number(removeEmpresaMatch[1]);
        const empresaId = Number(removeEmpresaMatch[2]);
        const group = agrupadores.find(g => g.id === agrupadorId);
        if (!group) {
          route.fulfill({ status: 404, body: '' });
          return;
        }
        empresaVinculadaPorGrupo.delete(empresaId);
        group.empresas = group.empresas.filter(e => e.empresaId !== empresaId);
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(group) });
        return;
      }

      const deleteGroupMatch = suffix.match(/^\/(\d+)$/);
      if (method === 'DELETE' && deleteGroupMatch) {
        const agrupadorId = Number(deleteGroupMatch[1]);
        const idx = agrupadores.findIndex(g => g.id === agrupadorId);
        if (idx >= 0) {
          for (const empresa of agrupadores[idx].empresas) {
            empresaVinculadaPorGrupo.delete(empresa.empresaId);
          }
          agrupadores.splice(idx, 1);
        }
        route.fulfill({ status: 204, body: '' });
        return;
      }

      route.fulfill({ status: 404, body: '' });
    });

    await page.goto('/configs');

    await page.getByLabel('ID da configuracao').fill('10');
    const cards = page.locator('article.group-card');
    await expect(cards).toHaveCount(2);

    await page.getByLabel('Novo agrupador').fill('Grupo C');
    await page.getByRole('button', { name: 'Criar' }).click();
    await expect(cards).toHaveCount(3);

    const groupA = cards.nth(0);
    const groupB = cards.nth(1);
    const filialNorteOption = page.locator('mat-option[role="option"]').filter({ hasText: 'Filial - Filial Norte' }).first();

    await groupA.getByRole('combobox', { name: 'Empresas' }).click();
    await filialNorteOption.click();
    await page.keyboard.press('Escape');

    await groupB.getByRole('combobox', { name: 'Empresas' }).click();
    await filialNorteOption.click();
    await page.keyboard.press('Escape');

    await expect(
      page.locator('.mat-mdc-snack-bar-label').filter({
        hasText: 'Esta empresa ja esta vinculada a outro agrupador nesta configuracao.'
      }).first()
    ).toBeVisible();
  });

  test('deve permitir reaproveitar empresa apos excluir agrupador', async ({ page }) => {
    const empresas: Empresa[] = [
      { id: 1, tipo: 'MATRIZ', razaoSocial: 'Matriz Zeta' },
      { id: 2, tipo: 'FILIAL', razaoSocial: 'Filial Norte', padrao: true }
    ];

    const agrupadores: Agrupador[] = [
      { id: 301, nome: 'Grupo A', ativo: true, empresas: [{ empresaId: 2, nome: 'Filial Norte' }] },
      { id: 302, nome: 'Grupo B', ativo: true, empresas: [] }
    ];
    const empresaVinculadaPorGrupo = new Map<number, number>([[2, 301]]);
    let addSuccessCount = 0;
    let conflictCount = 0;

    await page.route('**/api/me', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'e2e-user',
          username: 'e2e',
          roles: [],
          tenantRoles: ['MASTER', 'ADMIN'],
          permissions: ['CONFIG_EDITOR', 'RELATORIO_VIEW', 'ENTIDADE_EDIT'],
          tenantId: '1'
        })
      });
    });

    await page.route('**/api/access-controls**', route => {
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/atalhos**', route => {
      const method = route.request().method();
      if (method === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
        return;
      }
      route.fulfill({ status: 204, body: '' });
    });

    await page.route('**/api/me/empresa-padrao**', route => {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ empresaId: 2 }) });
    });

    await page.route('**/api/empresas?**', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: empresas,
          totalPages: 1,
          totalElements: empresas.length,
          number: 0,
          size: 100
        })
      });
    });

    await page.route('**/api/empresas/*', route => {
      const id = Number(route.request().url().split('/').pop() || '0');
      const empresa = empresas.find(e => e.id === id);
      if (!empresa) {
        route.fulfill({ status: 404, body: '' });
        return;
      }
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(empresa) });
    });

    await page.route('**/api/configuracoes/FORMULARIO/20/agrupadores-empresa**', route => {
      const method = route.request().method();
      const path = new URL(route.request().url()).pathname;
      const suffix = path.replace('/api/configuracoes/FORMULARIO/20/agrupadores-empresa', '');

      if (method === 'GET' && (suffix === '' || suffix === '/')) {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(agrupadores) });
        return;
      }

      const addEmpresaMatch = suffix.match(/^\/(\d+)\/empresas$/);
      if (method === 'POST' && addEmpresaMatch) {
        const agrupadorId = Number(addEmpresaMatch[1]);
        const body = route.request().postDataJSON() as { empresaId: number };
        const group = agrupadores.find(g => g.id === agrupadorId);
        if (!group) {
          route.fulfill({ status: 404, body: '' });
          return;
        }
        const usedBy = [...empresaVinculadaPorGrupo.entries()]
          .find(([empresaId, groupId]) => empresaId === body.empresaId && groupId !== agrupadorId);
        if (usedBy) {
          conflictCount++;
          route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              title: 'Conflito de integridade',
              detail: 'Esta empresa ja esta vinculada a outro agrupador nesta configuracao.'
            })
          });
          return;
        }
        addSuccessCount++;
        empresaVinculadaPorGrupo.set(body.empresaId, agrupadorId);
        if (!group.empresas.some(e => e.empresaId === body.empresaId)) {
          group.empresas.push({ empresaId: body.empresaId, nome: 'Filial Norte' });
        }
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(group) });
        return;
      }

      const deleteGroupMatch = suffix.match(/^\/(\d+)$/);
      if (method === 'DELETE' && deleteGroupMatch) {
        const agrupadorId = Number(deleteGroupMatch[1]);
        const idx = agrupadores.findIndex(g => g.id === agrupadorId);
        if (idx >= 0) {
          for (const empresa of agrupadores[idx].empresas) {
            empresaVinculadaPorGrupo.delete(empresa.empresaId);
          }
          agrupadores.splice(idx, 1);
        }
        route.fulfill({ status: 204, body: '' });
        return;
      }

      route.fulfill({ status: 404, body: '' });
    });

    await page.goto('/configs');
    await page.getByLabel('ID da configuracao').fill('20');

    const cards = page.locator('article.group-card');
    await expect(cards).toHaveCount(2);

    const groupA = cards.nth(0);
    await groupA.getByRole('button', { name: 'Remover' }).click();
    await expect(cards).toHaveCount(1);

    const remainingGroup = cards.nth(0);
    const filialNorteOption = page.locator('mat-option[role="option"]').filter({ hasText: 'Filial - Filial Norte' }).first();
    await remainingGroup.getByRole('combobox', { name: 'Empresas' }).click();
    await filialNorteOption.click();
    await page.keyboard.press('Escape');

    await expect.poll(() => addSuccessCount).toBe(1);
    await expect.poll(() => conflictCount).toBe(0);
  });
});

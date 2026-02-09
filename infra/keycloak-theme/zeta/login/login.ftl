<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=true; section>
  <#if section == "header">
    <div class="zeta-brand">
      <img src="${url.resourcesPath}/img/logo-zeta.png" alt="Zeta Informatica" />
      <div class="zeta-title">Zeta Informatica</div>
      <div class="zeta-sub">Acesso ao sistema</div>
    </div>
  <#elseif section == "form">
    <#nested "form">
  <#elseif section == "info">
    <#nested "info">
  </#if>
</@layout.registrationLayout>

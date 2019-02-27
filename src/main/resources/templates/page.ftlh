<#include "macros.ftlh">
<@htmlBase pageTitle siteName!"Wsite">
<#if escape!true>
${contents}
<#else>
${contents?no_esc}
</#if>
</@htmlBase>
<#include "macros.ftlh">
<@htmlBase "An exception has occurred!" siteName>
<section id="exception">
  <h1>An exception has occurred!</h1>
  <h3>${message!"<No message>"}</h3>
  <pre>${stacktrace!"<No stacktrace>"}</pre>
</section>
</@htmlBase>

(function(){

  const INJECTED_ID = 'firefox-webext-content-injection';

  function injectContent() {
      document.body.style.border = "5px solid red";
      const div = document.createElement("div");
      div.id = INJECTED_ID;
      div.style.position = "fixed";
      div.style.top = 0;
      div.style.left = 0;
      div.style.width = "64px";
      div.style.height = "64px";
      div.innerText = "Hello, world";
      div.style.borderRadius = 0;
      div.style.padding = '5px';
      document.body.appendChild(div);
  }
  document.addEventListener("DOMContentLoaded", injectContent);
})();


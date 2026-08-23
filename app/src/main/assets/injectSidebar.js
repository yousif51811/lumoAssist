// Most of this here is AI Generated code
// i don't know javascript too well
// Uses the actual button object in html, just duplicates it.

(function () {
    const BUTTON_ID = "lumo-assist-custom-button";

    function addCustomButton() {
        if (document.getElementById(BUTTON_ID)) {
            return true;
        }

        const section = document.querySelector(".sidebar-section");

        if (!section) {
            return false;
        }

        const button = document.createElement("button");

        button.id = BUTTON_ID;
        button.type = "button";
        button.className =
            "sidebar-item flex items-center w-full cursor-pointer px-1.5 py-2";
        button.setAttribute("aria-label", "lumoAssist Options");

        // No icon for now.
        button.innerHTML = `
            <div class="sidebar-item-icon flex items-center justify-start shrink-0" aria-hidden="true">
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    width="20"
                    height="20"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                >
                    <circle cx="12" cy="12" r="8.5"></circle>
                    <path d="M8 8v3"></path>
                    <path d="M8 13v3"></path>
                    <path d="M16 8v3"></path>
                    <path d="M16 13v3"></path>
                    <path d="M6.5 11h3"></path>
                    <path d="M14.5 13h3"></path>
                </svg>
            </div>

            <span class="sidebar-item-text flex-1 flex items-center gap-2">
                <span class="sidebar-item-label">lumoAssist Options</span>
            </span>
        `;


        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            if (window.AndroidInterface) {
                window.AndroidInterface.openOptions();
            }
        });

        const imagesButton =
            section.querySelector('button[aria-label="Images"]');

        if (imagesButton) {
            section.insertBefore(button, imagesButton);
        } else {
            section.insertBefore(button, section.firstChild);
        }

        return true;
    }

    if (!addCustomButton()) {
        const observer = new MutationObserver(function () {
            if (addCustomButton()) {
                observer.disconnect();
            }
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
})();

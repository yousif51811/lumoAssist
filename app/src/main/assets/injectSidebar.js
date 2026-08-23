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
        button.setAttribute("aria-label", "My Options");

        // No icon for now.
        button.innerHTML =
            '<div class="sidebar-item-icon"></div>' +
            '<span class="sidebar-item-text flex-1 flex items-center gap-2">' +
                '<span class="sidebar-item-label">My Options</span>' +
            "</span>";

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

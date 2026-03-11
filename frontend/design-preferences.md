# Design Preferences

## Input Field

Use the following styling as the reference for input fields:

```html
<div class="max-w-sm w-full space-y-3">
  <input
    id="input-base"
    type="text"
    class="py-2.5 sm:py-3 px-4 rounded-lg block w-full bg-layer border-layer-line sm:text-sm text-foreground placeholder:text-muted-foreground-1 focus:border-primary-focus focus:ring-primary-focus disabled:opacity-50 disabled:pointer-events-none"
    placeholder="This is placeholder"
  >
</div>
```

## Action Alert

Use the following styling as the reference for alerts that present an action:

```html
<div
  class="bg-primary-100 border action-alert-border text-foreground rounded-lg p-4 dark:bg-primary-500/20"
  role="alert"
  tabindex="-1"
  aria-labelledby="hs-actions-label"
>
  <div class="flex">
    <div class="shrink-0">
      <svg
        class="shrink-0 size-4 mt-1"
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="10" />
        <path d="M12 16v-4" />
        <path d="M12 8h.01" />
      </svg>
    </div>
    <div class="ms-3">
      <h3 id="hs-actions-label" class="font-semibold">
        Heading
      </h3>
      <div class="mt-2 text-sm text-muted-foreground-2">
        Supporting copy for the action.
      </div>
      <div class="mt-4">
        <div class="flex gap-x-3">
          <button
            type="button"
            class="inline-flex items-center gap-x-2 text-sm font-semibold rounded-lg border border-transparent text-primary hover:text-primary-hover focus:outline-hidden focus:text-primary-focus disabled:opacity-50 disabled:pointer-events-none"
          >
            Primary action
          </button>
        </div>
      </div>
    </div>
  </div>
</div>
```

import type { Config } from "tailwindcss"

const config = {
    darkMode: ["class"],
    content: [
        "./pages/**/*.{ts,tsx}",
        "./components/**/*.{ts,tsx}",
        "./app/**/*.{ts,tsx}",
        "./src/**/*.{ts,tsx}",
        "*.{js,ts,jsx,tsx,mdx}",
    ],
    prefix: "",
    theme: {
        container: {
            center: true,
            padding: "2rem",
            screens: {
                "2xl": "1400px",
            },
        },
        extend: {
            colors: {
                border: "hsl(var(--border))",
                input: "hsl(var(--input))",
                ring: "hsl(var(--ring))",
                background: "hsl(var(--background))",
                foreground: "hsl(var(--foreground))",
                primary: {
                    DEFAULT: "hsl(var(--primary))",
                    foreground: "hsl(var(--primary-foreground))",
                },
                secondary: {
                    DEFAULT: "hsl(var(--secondary))",
                    foreground: "hsl(var(--secondary-foreground))",
                },
                destructive: {
                    DEFAULT: "hsl(var(--destructive))",
                    foreground: "hsl(var(--destructive-foreground))",
                },
                muted: {
                    DEFAULT: "hsl(var(--muted))",
                    foreground: "hsl(var(--muted-foreground))",
                },
                accent: {
                    DEFAULT: "hsl(var(--accent))",
                    foreground: "hsl(var(--accent-foreground))",
                },
                popover: {
                    DEFAULT: "hsl(var(--popover))",
                    foreground: "hsl(var(--popover-foreground))",
                },
                card: {
                    DEFAULT: "hsl(var(--card))",
                    foreground: "hsl(var(--card-foreground))",
                },
            },
            borderRadius: {
                lg: "var(--radius)",
                md: "calc(var(--radius) - 2px)",
                sm: "calc(var(--radius) - 4px)",
            },
            keyframes: {
                "accordion-down": {
                    from: { height: "0" },
                    to: { height: "var(--radix-accordion-content-height)" },
                },
                "accordion-up": {
                    from: { height: "var(--radix-accordion-content-height)" },
                    to: { height: "0" },
                },
            },
            animation: {
                "accordion-down": "accordion-down 0.2s ease-out",
                "accordion-up": "accordion-up 0.2s ease-out",
            },
            typography: {
                DEFAULT: {
                    css: {
                        maxWidth: "100%",
                        color: "var(--tw-prose-body)",
                        a: {
                            color: "var(--tw-prose-links)",
                            textDecoration: "underline",
                            fontWeight: "500",
                        },
                        h1: {
                            color: "var(--tw-prose-headings)",
                            fontWeight: "700",
                            fontSize: "2.25em",
                            marginBottom: "0.8888889em",
                            lineHeight: "1.1111111",
                        },
                        h2: {
                            color: "var(--tw-prose-headings)",
                            fontWeight: "700",
                            fontSize: "1.5em",
                            marginTop: "2em",
                            marginBottom: "1em",
                            lineHeight: "1.3333333",
                        },
                        code: {
                            color: "var(--tw-prose-code)",
                            fontWeight: "600",
                        },
                        "code::before": {
                            content: '""',
                        },
                        "code::after": {
                            content: '""',
                        },
                        pre: {
                            color: "var(--tw-prose-pre-code)",
                            backgroundColor: "var(--tw-prose-pre-bg)",
                            borderRadius: "0.375rem",
                            padding: "1rem",
                            overflowX: "auto",
                        },
                    },
                },
            },
        },
    },
    plugins: [require("tailwindcss-animate"), require("@tailwindcss/typography")],
} satisfies Config

export default config


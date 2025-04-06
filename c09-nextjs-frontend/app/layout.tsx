import type {Metadata} from "next";
import {Providers} from "./providers";

import "./globals.css";
import React from "react";

export const metadata: Metadata = {
    title: "Zoom BMP",
    description: "Zoom BMP is a simple image viewer that lets you zoom in and out of images.",
};

export default function RootLayout({children}: Readonly<{ children: React.ReactNode; }>) {
    return (
        <html lang="en" className="dark">
        <body>
        <Providers>
            {children}
        </Providers>
        </body>
        </html>
    );
}

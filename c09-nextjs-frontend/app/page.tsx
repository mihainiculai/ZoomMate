"use client";

import React, { ChangeEvent, FormEvent, useEffect, useRef, useState } from "react";
import { Alert, Button, Card, Image, Input, Slider, Spacer } from "@heroui/react";
import axios, { AxiosError } from "axios";
import ContainerStats from "./_components/container-stats";

interface UploadResponse {
    jobId: string;
    message: string;
}

interface WebSocketMessage {
    jobId: string;
    downloadUrl: string;
}

interface ImageProcessingError {
    message: string;
    status?: number;
}

export default function Home() {
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [zoomLevel, setZoomLevel] = useState<number>(1);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [jobId, setJobId] = useState<string | null>(null);
    const [processedImageUrl, setProcessedImageUrl] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [originalSize, setOriginalSize] = useState<number | null>(null);
    const [originalResolution, setOriginalResolution] = useState<{ width: number; height: number } | null>(null);
    const [processedSize, setProcessedSize] = useState<number | null>(null);
    const [processedResolution, setProcessedResolution] = useState<{ width: number; height: number } | null>(null);

    const timeoutRef = useRef<NodeJS.Timeout | null>(null);

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file && file.type === "image/bmp") {
            setSelectedFile(file);
            setError(null);

            const sizeMB = file.size / (1024 * 1024);
            setOriginalSize(sizeMB);

            const reader = new FileReader();
            reader.onload = (e) => {
                const img = new window.Image();
                img.onload = () => {
                    setOriginalResolution({ width: img.naturalWidth, height: img.naturalHeight });
                };
                img.src = e.target?.result as string;
            };
            reader.readAsDataURL(file);
        } else {
            setSelectedFile(null);
            setError("Please select a valid BMP image.");
        }
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!selectedFile) {
            setError("Please select an image.");
            return;
        }

        setProcessedImageUrl(null);
        setError(null);
        setIsLoading(true);

        const formData = new FormData();
        formData.append("image", selectedFile);
        formData.append("zoomLevel", zoomLevel.toString());

        try {
            const response = await axios.post<UploadResponse>(
                "http://localhost:8081/api/upload",
                formData,
                {
                    headers: { "Content-Type": "multipart/form-data" },
                }
            );
            setJobId(response.data.jobId);
            console.log("Job ID:", response.data.jobId);
        } catch (err) {
            console.error("Error processing image:", err);
            const axiosError = err as AxiosError<ImageProcessingError>;
            setError(
                axiosError.response?.data?.message ||
                "An error occurred while processing the image."
            );
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (!jobId) return;

        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }

        // Timeout to handle unresponsive server
        timeoutRef.current = setTimeout(() => {
            setIsLoading(false);
            setError("Server timeout. Please try again.");
        }, 10000);

        const ws = new WebSocket("ws://0.0.0.0:8081/ws");

        ws.onopen = () => {
            console.log("WebSocket connected");
        };

        ws.onmessage = (event) => {
            console.log("WebSocket message:", event.data);
            try {
                const data: WebSocketMessage = JSON.parse(event.data);
                if (data.jobId === jobId) {
                    if (timeoutRef.current) {
                        clearTimeout(timeoutRef.current);
                        timeoutRef.current = null;
                    }
                    setIsLoading(false);
                    setProcessedImageUrl(data.downloadUrl);
                }
            } catch (parseError) {
                console.error("Error parsing WebSocket message:", parseError);
            }
        };

        ws.onclose = () => {
            console.log("WebSocket closed");
        };

        return () => {
            ws.close();
        };
    }, [jobId]);

    useEffect(() => {
        if (!processedImageUrl) return;

        // Get processed image size
        fetch(processedImageUrl)
            .then(res => res.blob())
            .then(blob => {
                const sizeMB = blob.size / (1024 * 1024);
                setProcessedSize(sizeMB);
            });

        // Get processed image dimensions
        const img = new window.Image();
        img.onload = () => {
            setProcessedResolution({ width: img.naturalWidth, height: img.naturalHeight });
        };
        img.src = processedImageUrl;
    }, [processedImageUrl]);

    return (
        <div className="min-h-screen p-16 flex items-start justify-center space-x-8">
            <Card className="p-8 space-y-6 w-full basis-1/2">
                <h1 className="text-2xl font-bold text-center">ZoomMate</h1>
                <Spacer y={8} />

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <Input
                            type="file"
                            accept=".bmp"
                            onChange={handleFileChange}
                            className="w-full"
                            label="Select image"
                            description="Only BMP files are allowed"
                            isInvalid={!!error && !selectedFile}
                            errorMessage={!selectedFile ? error : undefined}
                        />
                    </div>

                    <div>
                        <Slider
                            label="Zoom Level"
                            value={zoomLevel}
                            minValue={0.1}
                            maxValue={5}
                            step={0.1}
                            onChange={(value) =>
                                setZoomLevel(Array.isArray(value) ? value[0] : value)
                            }
                            defaultValue={1}
                            marks={[
                                { value: 1, label: "1x" },
                                { value: 2.5, label: "2.5x" },
                                { value: 5, label: "5x" },
                            ]}
                            showTooltip
                        />
                    </div>

                    <Spacer y={10} />
                    <Button
                        color="primary"
                        type="submit"
                        className="w-full"
                        isLoading={isLoading}
                        isDisabled={!selectedFile || isLoading}
                    >
                        {isLoading ? "Processing..." : "Process Image"}
                    </Button>
                </form>

                {error && !isLoading && <Alert color="danger" className="p-4" title={error} />}

                {processedImageUrl && (
                    <div className="mt-6">
                        <h2 className="text-lg font-semibold mb-2">Processed Image</h2>
                        <Image
                            src={processedImageUrl}
                            alt="Processed Image"
                            className="max-w-full h-auto rounded-lg shadow-lg"
                        />
                        <Alert
                            className="mt-4"
                            color="success"
                            title = "Image Processed Successfully"
                        >
                            <div className="text-sm">
                                Size: <b>{originalSize?.toFixed(2)} MB</b> -&gt; <b>{processedSize?.toFixed(2)} MB</b>
                                <br />
                                Resolution: <b>{originalResolution?.width}x{originalResolution?.height}</b> -&gt; <b>{processedResolution?.width}x{processedResolution?.height}</b>
                            </div>
                        </Alert>
                        <Button
                            color="secondary"
                            className="mt-4 w-full"
                            onPress={() => window.open(processedImageUrl, "_blank")}
                        >
                            Download Image
                        </Button>
                    </div>
                )}
            </Card>

            <ContainerStats />
        </div>
    );
}

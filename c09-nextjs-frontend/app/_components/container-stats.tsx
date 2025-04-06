"use client";

import React, {useEffect, useState} from "react";
import {Accordion, AccordionItem, Alert, Card, Spacer} from "@heroui/react";
import {Area, AreaChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis,} from "recharts";
import axios from "axios";

interface ContainerData {
    container: string;
    timestamp: string;
    cpuLoad1: string;
    memTotal: string;
    memFree: string;
    sysName: string;
    sysDescr: string;
}

const REFRESH_INTERVAL = 10000;

export default function ContainerStats() {
    const [containerStatsData, setContainerStatsData] = useState<ContainerData[]>([]);
    const [error, setError] = useState<string | null>(null);

    const fetchData = async () => {
        try {
            const response = await axios.get("http://0.0.0.0:3001/api/snmp?minutes=10");
            if (response.data.length !== 0) {
                setContainerStatsData(response.data);
            }
            setError(null);
        } catch (fetchError) {
            setError("Failed to fetch container stats.");
            console.error(fetchError);
        }
    };

    useEffect(() => {
        fetchData();
        const intervalId = setInterval(fetchData, REFRESH_INTERVAL);
        return () => clearInterval(intervalId);
    }, []);

    const formatCPUDataForChart = () => {
        const groupedData: Record<string, { timestamp: string; value: number }[]> = {};

        containerStatsData.forEach((entry) => {
            const containerName = entry.container;
            if (!groupedData[containerName]) {
                groupedData[containerName] = [];
            }
            groupedData[containerName].push({
                timestamp: new Date(entry.timestamp).toLocaleTimeString(),
                value: parseFloat(entry.cpuLoad1 || "0"),
            });
        });

        return groupedData;
    };

    const formatMemoryDataForChart = () => {
        const groupedData: Record<string, { timestamp: string; used: number }[]> = {};

        containerStatsData.forEach((entry) => {
            const containerName = entry.container;
            if (!groupedData[containerName]) {
                groupedData[containerName] = [];
            }
            const memTotalNum = parseFloat(entry.memTotal || "0");
            const memFreeNum = parseFloat(entry.memFree || "0");
            const memUsedMB = (memTotalNum - memFreeNum) / 1024;

            groupedData[containerName].push({
                timestamp: new Date(entry.timestamp).toLocaleTimeString(),
                used: memUsedMB,
            });
        });

        return groupedData;
    };

    const cpuChartData = formatCPUDataForChart();
    const memoryChartData = formatMemoryDataForChart();

    const allContainerNames = new Set([
        ...Object.keys(cpuChartData),
        ...Object.keys(memoryChartData),
    ]);
    const sortedContainers = Array.from(allContainerNames).sort();

    return (
        <div className="p-8 space-y-6 w-full basis-1/2">
            <h1 className="text-2xl font-bold text-center">Container Resource Usage</h1>
            <Spacer y={2}/>

            {error && (
                <Alert color="danger" title={error} />
            )}

            {!error && (
                <Accordion selectionMode="multiple">
                    {sortedContainers.map((containerName) => {
                        const cpuDataForContainer = cpuChartData[containerName] || [];
                        const memoryDataForContainer = memoryChartData[containerName] || [];

                        const fullContainerData = containerStatsData.filter(
                            (record) => record.container === containerName
                        );
                        const lastRecord = fullContainerData[fullContainerData.length - 1];

                        let totalMemoryMB = 0;
                        if (lastRecord) {
                            totalMemoryMB = parseFloat(lastRecord.memTotal) / 1024;
                        }

                        return (
                            <AccordionItem
                                key={containerName}
                                aria-label={`Accordion ${containerName}`}
                                title={containerName}
                            >
                                {lastRecord && lastRecord.sysDescr && (
                                    <div>
                                        <p>
                                            <strong>Hostname: </strong>
                                            {lastRecord.sysName}
                                        </p>
                                        <p>
                                            <strong>System: </strong>
                                            {lastRecord.sysDescr}
                                        </p>
                                        <p>
                                            <strong>Total Memory: </strong>
                                            {totalMemoryMB.toFixed(0)} MB
                                        </p>
                                    </div>
                                )}

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                    <Card className="p-6 space-y-4">
                                        <h2 className="text-lg font-semibold">CPU Load</h2>
                                        <ResponsiveContainer width="100%" height={200}>
                                            <AreaChart
                                                data={cpuDataForContainer}
                                                margin={{left: 0, right: 0}}
                                            >
                                                <defs>
                                                    <linearGradient
                                                        id={`cpuColor-${containerName}`}
                                                        x1="0"
                                                        y1="0"
                                                        x2="0"
                                                        y2="1"
                                                    >
                                                        <stop offset="5%" stopColor="#8884d8" stopOpacity={0.8}/>
                                                        <stop offset="95%" stopColor="#8884d8" stopOpacity={0}/>
                                                    </linearGradient>
                                                </defs>
                                                <XAxis
                                                    dataKey="timestamp"
                                                    tickFormatter={(tick) => {
                                                        const [hour, minute] = tick.split(":");
                                                        return `${hour}:${minute}`;
                                                    }}
                                                />
                                                <YAxis
                                                    label={{
                                                        value: "CPUs",
                                                        angle: -90,
                                                        position: "insideLeft",
                                                        style: {textAnchor: "middle"},
                                                    }}
                                                />
                                                <Tooltip/>
                                                <Area
                                                    type="monotone"
                                                    dataKey="value"
                                                    stroke="#8884d8"
                                                    fillOpacity={1}
                                                    fill={`url(#cpuColor-${containerName})`}
                                                />
                                            </AreaChart>
                                        </ResponsiveContainer>
                                    </Card>

                                    <Card className="p-6 space-y-4">
                                        <h2 className="text-lg font-semibold">Memory Used (MB)</h2>
                                        <ResponsiveContainer width="100%" height={200}>
                                            <AreaChart data={memoryDataForContainer}>
                                                <defs>
                                                    <linearGradient
                                                        id={`memColor-${containerName}`}
                                                        x1="0"
                                                        y1="0"
                                                        x2="0"
                                                        y2="1"
                                                    >
                                                        <stop offset="5%" stopColor="#82ca9d" stopOpacity={0.8}/>
                                                        <stop offset="95%" stopColor="#82ca9d" stopOpacity={0}/>
                                                    </linearGradient>
                                                </defs>
                                                <XAxis
                                                    dataKey="timestamp"
                                                    tickFormatter={(tick) => {
                                                        const [hour, minute] = tick.split(":");
                                                        return `${hour}:${minute}`;
                                                    }}
                                                />
                                                <YAxis
                                                    label={{
                                                        value: "MB",
                                                        angle: -90,
                                                        position: "insideLeft",
                                                        style: {textAnchor: "middle"},
                                                    }}
                                                />
                                                <Tooltip/>
                                                {totalMemoryMB > 0 && (
                                                    <ReferenceLine
                                                        y={totalMemoryMB}
                                                        stroke="red"
                                                        strokeDasharray="3 3"
                                                        label={`Total: ${totalMemoryMB.toFixed(0)} MB`}
                                                    />
                                                )}
                                                <Area
                                                    type="monotone"
                                                    dataKey="used"
                                                    stroke="#82ca9d"
                                                    fillOpacity={1}
                                                    fill={`url(#memColor-${containerName})`}
                                                />
                                            </AreaChart>
                                        </ResponsiveContainer>
                                    </Card>
                                </div>
                            </AccordionItem>
                        );
                    })}
                </Accordion>
            )}
        </div>
    );
}

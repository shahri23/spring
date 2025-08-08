import React, { useState, useEffect, useCallback } from 'react';
import { Search, Play, Download, RefreshCw, AlertCircle, CheckCircle, Clock, Server, Database, Cpu } from 'lucide-react';

const ContainerManagementGUI = () => {
    const [selectedTeam, setSelectedTeam] = useState('');
    const [selectedApp, setSelectedApp] = useState('');
    const [selectedPod, setSelectedPod] = useState('');
    const [selectedContainer, setSelectedContainer] = useState('');

    const [teams, setTeams] = useState([]);
    const [apps, setApps] = useState([]);
    const [pods, setPods] = useState([]);
    const [containers, setContainers] = useState([]);

    const [commands, setCommands] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Mock API base URL - replace with actual API endpoint
    const API_BASE = 'http://monitoring-api:8080/api';

    // Available commands
    const availableCommands = [
        { type: 'HEAP_DUMP', name: 'Create Heap Dump', description: 'Generate JVM heap dump' },
        { type: 'THREAD_DUMP', name: 'Create Thread Dump', description: 'Generate thread dump' },
        { type: 'GC_RUN', name: 'Run Garbage Collection', description: 'Force garbage collection' },
        { type: 'SYSTEM_INFO', name: 'Get System Info', description: 'Retrieve system information' }
    ];

    // Fetch data with error handling
    const fetchData = useCallback(async (url) => {
        try {
            const response = await fetch(`${API_BASE}${url}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return await response.json();
        } catch (err) {
            console.error(`Failed to fetch ${url}:`, err);
            setError(`Failed to load data: ${err.message}`);
            return [];
        }
    }, []);

    // Load teams on component mount
    useEffect(() => {
        const loadTeams = async () => {
            setLoading(true);
            const teamsData = await fetchData('/teams');
            setTeams(teamsData);
            setLoading(false);
        };
        loadTeams();
    }, [fetchData]);

    // Load apps when team changes
    useEffect(() => {
        if (selectedTeam) {
            const loadApps = async () => {
                const appsData = await fetchData(`/teams/${selectedTeam}/apps`);
                setApps(appsData);
                setSelectedApp('');
                setPods([]);
                setContainers([]);
                setSelectedPod('');
                setSelectedContainer('');
            };
            loadApps();
        } else {
            setApps([]);
            setPods([]);
            setContainers([]);
        }
    }, [selectedTeam, fetchData]);

    // Load pods when app changes
    useEffect(() => {
        if (selectedTeam && selectedApp) {
            const loadPods = async () => {
                const podsData = await fetchData(`/teams/${selectedTeam}/apps/${selectedApp}/pods`);
                setPods(podsData);
                setSelectedPod('');
                setContainers([]);
                setSelectedContainer('');
            };
            loadPods();
        } else {
            setPods([]);
            setContainers([]);
        }
    }, [selectedApp, selectedTeam, fetchData]);

    // Load containers when pod changes
    useEffect(() => {
        if (selectedTeam && selectedApp && selectedPod) {
            const loadContainers = async () => {
                const containersData = await fetchData(`/teams/${selectedTeam}/apps/${selectedApp}/pods/${selectedPod}/containers`);
                setContainers(containersData);
                setSelectedContainer('');
            };
            loadContainers();
        } else {
            setContainers([]);
        }
    }, [selectedPod, selectedTeam, selectedApp, fetchData]);

    // Load command history when container changes
    useEffect(() => {
        if (selectedContainer) {
            const loadCommands = async () => {
                const commandsData = await fetchData(`/containers/${selectedContainer}/commands/history`);
                setCommands(commandsData);
            };
            loadCommands();

            // Set up polling for command updates
            const interval = setInterval(loadCommands, 5000);
            return () => clearInterval(interval);
        } else {
            setCommands([]);
        }
    }, [selectedContainer, fetchData]);

    // Execute command
    const executeCommand = async (commandType) => {
        if (!selectedContainer) {
            setError('Please select a container first');
            return;
        }

        setLoading(true);
        try {
            const response = await fetch(`${API_BASE}/containers/${selectedContainer}/command`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    type: commandType,
                    parameters: {}
                })
            });

            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const result = await response.json();
            if (result.success) {
                // Refresh command history
                const commandsData = await fetchData(`/containers/${selectedContainer}/commands/history`);
                setCommands(commandsData);
                setError(null);
            } else {
                setError(result.message || 'Command execution failed');
            }
        } catch (err) {
            setError(`Failed to execute command: ${err.message}`);
        }
        setLoading(false);
    };

    // Download file
    const downloadFile = async (fileId, fileName) => {
        try {
            const response = await fetch(`${API_BASE}/files/${fileId}/download`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            setError(`Failed to download file: ${err.message}`);
        }
    };

    // Get status icon
    const getStatusIcon = (status) => {
        switch (status) {
            case 'completed': return <CheckCircle className="w-4 h-4 text-green-500" />;
            case 'failed': return <AlertCircle className="w-4 h-4 text-red-500" />;
            case 'processing': return <RefreshCw className="w-4 h-4 text-blue-500 animate-spin" />;
            default: return <Clock className="w-4 h-4 text-yellow-500" />;
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
            <div className="container mx-auto p-6">
                <div className="mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2">Container Management System</h1>
                    <p className="text-gray-300">Monitor and manage Java containers in AKS</p>
                </div>

                {error && (
                    <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 mb-6">
                        <div className="flex items-center">
                            <AlertCircle className="w-5 h-5 text-red-400 mr-2" />
                            <span className="text-red-200">{error}</span>
                            <button
                                onClick={() => setError(null)}
                                className="ml-auto text-red-300 hover:text-red-100"
                            >
                                ×
                            </button>
                        </div>
                    </div>
                )}

                <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                    {/* Selection Panel */}
                    <div className="xl:col-span-1">
                        <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700/50">
                            <h2 className="text-xl font-semibold text-white mb-4 flex items-center">
                                <Search className="w-5 h-5 mr-2" />
                                Select Container
                            </h2>

                            <div className="space-y-4">
                                {/* Team Selection */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">Team</label>
                                    <select
                                        value={selectedTeam}
                                        onChange={(e) => setSelectedTeam(e.target.value)}
                                        className="w-full bg-slate-700/50 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                    >
                                        <option value="">Select Team</option>
                                        {teams.map((team) => (
                                            <option key={team} value={team}>{team}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* App Selection */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">Application</label>
                                    <select
                                        value={selectedApp}
                                        onChange={(e) => setSelectedApp(e.target.value)}
                                        disabled={!selectedTeam}
                                        className="w-full bg-slate-700/50 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50"
                                    >
                                        <option value="">Select Application</option>
                                        {apps.map((app) => (
                                            <option key={app} value={app}>{app}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Pod Selection */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">Pod</label>
                                    <select
                                        value={selectedPod}
                                        onChange={(e) => setSelectedPod(e.target.value)}
                                        disabled={!selectedApp}
                                        className="w-full bg-slate-700/50 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50"
                                    >
                                        <option value="">Select Pod</option>
                                        {pods.map((pod) => (
                                            <option key={pod} value={pod}>{pod}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Container Selection */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">Container</label>
                                    <select
                                        value={selectedContainer}
                                        onChange={(e) => setSelectedContainer(e.target.value)}
                                        disabled={!selectedPod}
                                        className="w-full bg-slate-700/50 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50"
                                    >
                                        <option value="">Select Container</option>
                                        {containers.map((container) => (
                                            <option key={container.id} value={container.id}>
                                                {container.containerName} ({container.status})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            {/* Container Info */}
                            {selectedContainer && containers.find(c => c.id === selectedContainer) && (
                                <div className="mt-6 p-4 bg-slate-700/30 rounded-lg">
                                    <h3 className="text-sm font-medium text-gray-300 mb-2">Container Details</h3>
                                    <div className="space-y-2 text-sm">
                                        {(() => {
                                            const container = containers.find(c => c.id === selectedContainer);
                                            return (
                                                <>
                                                    <div className="flex items-center text-gray-400">
                                                        <Server className="w-4 h-4 mr-2" />
                                                        <span>Status: </span>
                                                        <span className={`ml-1 font-medium ${container.status === 'active' ? 'text-green-400' : 'text-red-400'
                                                            }`}>
                                                            {container.status}
                                                        </span>
                                                    </div>
                                                    <div className="flex items-center text-gray-400">
                                                        <Database className="w-4 h-4 mr-2" />
                                                        <span>Host: {container.hostIp}</span>
                                                    </div>
                                                    <div className="flex items-center text-gray-400">
                                                        <Cpu className="w-4 h-4 mr-2" />
                                                        <span>Last Heartbeat: {new Date(container.lastHeartbeat).toLocaleTimeString()}</span>
                                                    </div>
                                                </>
                                            );
                                        })()}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Commands and History Panel */}
                    <div className="xl:col-span-2">
                        <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700/50">
                            <h2 className="text-xl font-semibold text-white mb-4 flex items-center">
                                <Play className="w-5 h-5 mr-2" />
                                Commands & History
                            </h2>

                            {/* Command Buttons */}
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
                                {availableCommands.map((command) => (
                                    <button
                                        key={command.type}
                                        onClick={() => executeCommand(command.type)}
                                        disabled={!selectedContainer || loading}
                                        className="p-4 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 disabled:from-gray-600 disabled:to-gray-700 rounded-lg transition-all duration-200 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        <div className="text-left">
                                            <div className="font-semibold">{command.name}</div>
                                            <div className="text-sm opacity-90">{command.description}</div>
                                        </div>
                                    </button>
                                ))}
                            </div>

                            {/* Command History */}
                            <div>
                                <h3 className="text-lg font-medium text-white mb-3">Command History</h3>
                                <div className="space-y-3 max-h-96 overflow-y-auto">
                                    {commands.length === 0 ? (
                                        <div className="text-center text-gray-400 py-8">
                                            {selectedContainer ? 'No commands executed yet' : 'Select a container to view command history'}
                                        </div>
                                    ) : (
                                        commands.map((command) => (
                                            <div key={command.id} className="bg-slate-700/30 rounded-lg p-4">
                                                <div className="flex items-center justify-between mb-2">
                                                    <div className="flex items-center">
                                                        {getStatusIcon(command.status)}
                                                        <span className="ml-2 font-medium text-white">{command.commandType}</span>
                                                    </div>
                                                    <span className="text-sm text-gray-400">
                                                        {new Date(command.createdAt).toLocaleString()}
                                                    </span>
                                                </div>

                                                {command.result && (
                                                    <div className="mt-2">
                                                        {command.result.message && (
                                                            <p className="text-sm text-gray-300 mb-2">{command.result.message}</p>
                                                        )}

                                                        {command.result.properties && command.result.properties.fileId && (
                                                            <button
                                                                onClick={() => downloadFile(
                                                                    command.result.properties.fileId,
                                                                    command.result.properties.fileName
                                                                )}
                                                                className="flex items-center text-sm text-blue-400 hover:text-blue-300 transition-colors"
                                                            >
                                                                <Download className="w-4 h-4 mr-1" />
                                                                Download {command.result.properties.fileName}
                                                                <span className="ml-2 text-gray-500">
                                                                    ({Math.round(command.result.properties.fileSize / 1024)} KB)
                                                                </span>
                                                            </button>
                                                        )}

                                                        {command.result.properties && !command.result.properties.fileId && (
                                                            <div className="text-xs text-gray-400 font-mono">
                                                                <pre className="whitespace-pre-wrap">
                                                                    {JSON.stringify(command.result.properties, null, 2)}
                                                                </pre>
                                                            </div>
                                                        )}

                                                        {command.result.errorMessage && (
                                                            <p className="text-sm text-red-400 mt-1">Error: {command.result.errorMessage}</p>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ContainerManagementGUI;
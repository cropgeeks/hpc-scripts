<template>
      
  <b-row v-bind:style= "[node.online ? {} : { 'opacity': 0.3 }]">
    
    <b-col cols=12 md=4 lg=3>
      {{ node.name }}
    </b-col>
    
    <b-col class="pr-0" v-b-tooltip.hover.html :title="cpuTooltip">
      <b-progress class="position-relative" :max="100" show-progress>        
        <b-progress-bar :value="node.usage" :style="{ 'background-color': '#dea36a', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100">
            {{ cpuPercent }}
          </div>
        </b-progress-bar>
        <b-progress-bar :value="cpuAllocPC(node)" :style="{ 'background-color': '#dea36a44', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100"/>
        </b-progress-bar>        
      </b-progress>
    </b-col>
  
    <b-col v-b-tooltip.hover.html :title="memTooltip">
      <b-progress class="position-relative" :max="node.memTotal" show-progress>        
        <b-progress-bar :value="node.memUsed" :style="{ 'background-color': '#1abc9c', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100">
            {{ memPercent }}
          </div>
        </b-progress-bar>
        <b-progress-bar :value="memAllocPC(node)" :style="{ 'background-color': '#1abc9c44', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100"/>
        </b-progress-bar>
      </b-progress>
    </b-col>
  
  </b-row>
  
</template>

<script>
export default {
  name: "NodeStats",
  props: {
    node: Object,
    showCPUs: null
  },

  computed: {
    memTooltip() {
      if (this.node.online === false)
        return "";

      var usedGB = (this.node.memUsed / 1024 / 1024).toFixed(1);
      var usedPC = ((this.node.memUsed / this.node.memTotal) * 100).toFixed(1);
      var allocGB = (this.node.memAlloc / 1024 / 1024).toFixed(1);
      var allocPC = ((this.node.memAlloc / this.node.memTotal) * 100).toFixed(1);
      var totalGB = (this.node.memTotal / 1024 / 1024).toFixed(1);

      return usedPC + "% used (" + usedGB + " GB)<br>"
        + allocPC + "% allocated (" + allocGB + " GB)<br>"
        + totalGB + " GB total";
    },

    cpuPercent() {
      return this.node.usage.toFixed(1);
    },

    cpuTooltip() {
      if (this.node.online === false)
        return "";

      return (this.node.usage).toFixed(1) + "% used<br>"
        + (this.node.alloc / this.node.cpus * 100).toFixed(1) + "% allocated<br>"
        + this.node.alloc + "/" + this.node.cpus + " CPUs<br>"
        + this.node.watts.toLocaleString() + " watts";
    },

    memPercent() {
      if (this.node.online)
        return ((this.node.memUsed / this.node.memTotal) * 100).toFixed(1);
      else
        return "0.0";
    }
  },

  methods: {
    cpuAllocPC(node) {
      if (node.alloc === 0)
        return 0;

      return (node.alloc / node.cpus * 100) - node.usage;
    },
    memAllocPC(node) {
      if (node.memAlloc === 0)
        return 0;

      return node.memAlloc-node.memUsed;
    }
  }
}
</script>
